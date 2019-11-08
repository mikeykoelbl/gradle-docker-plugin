/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.docker.tasks.image

import com.bmuschko.gradle.docker.DockerRegistryCredentials
import com.bmuschko.gradle.docker.tasks.AbstractDockerRemoteApiTask
import com.bmuschko.gradle.docker.tasks.RegistryCredentialsAware
import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.core.command.PullImageResultCallback
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

@CompileStatic
class DockerPullImage extends AbstractDockerRemoteApiTask implements RegistryCredentialsAware {

    /**
     * The image including repository, image name and tag used e.g. {@code vieux/apache:2.0}.
     *
     * @since 6.0.0
     */
    @Input
    final Property<String> image = project.objects.property(String)

    /**
     * {@inheritDoc}
     */
    final DockerRegistryCredentials registryCredentials

    DockerPullImage() {
        registryCredentials = project.objects.newInstance(DockerRegistryCredentials, project.objects)
    }

    @Override
    void runRemoteCommand() {
        logger.quiet "Pulling image '${image.get()}'."
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image.get())

        AuthConfig authConfig = getRegistryAuthLocator().lookupAuthConfig(image.get(), registryCredentials)
        pullImageCmd.withAuthConfig(authConfig)
        PullImageResultCallback callback = createCallback(nextHandler)
        pullImageCmd.exec(callback).awaitCompletion()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void registryCredentials(Action<? super DockerRegistryCredentials> action) {
        action.execute(registryCredentials)
    }

    private PullImageResultCallback createCallback(Action nextHandler) {
        new PullImageResultCallback() {
            @Override
            void onNext(PullResponseItem item) {
                if (nextHandler) {
                    try {
                        nextHandler.execute(item)
                    } catch (Exception e) {
                        logger.error('Failed to handle pull response', e)
                        return
                    }
                }
                super.onNext(item)
            }
        }
    }
}
