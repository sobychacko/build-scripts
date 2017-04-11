package io.springframework.cloud.ci

import io.springframework.cloud.common.SpringCloudJobs
import io.springframework.cloud.common.SpringCloudNotification
import io.springframework.common.job.Cron
import io.springframework.common.job.JdkConfig
import io.springframework.common.job.Maven
import io.springframework.common.job.SlackPlugin
import io.springframework.common.job.TestPublisher
import javaposse.jobdsl.dsl.DslFactory

/**
 * @author Marcin Grzejszczak
 */
class SpringCloudPipelinesDeployBuildMaker implements SpringCloudNotification, JdkConfig, TestPublisher, Cron,
		SpringCloudJobs, Maven {
	private final DslFactory dsl
	final String organization
	final String project

	SpringCloudPipelinesDeployBuildMaker(DslFactory dsl) {
		this.dsl = dsl
		this.organization = 'spring-cloud'
		this.project = "spring-cloud-pipelines"
	}

	void deploy() {
		dsl.job("${project}-${masterBranch()}-ci") {
			triggers {
				cron everyThreeHours()
				githubPush()
			}
			jdk jdk8()
			scm {
				git {
					remote {
						url "https://github.com/${organization}/${project}"
						branch masterBranch()
					}
					extensions {
						wipeOutWorkspace()
					}
				}
			}
			wrappers {
				timestamps()
				colorizeOutput()
				maskPasswords()
				credentialsBinding {
					usernamePassword(githubRepoUserNameEnvVar(),
							githubRepoPasswordEnvVar(),
							githubUserCredentialId())
					usernamePassword(dockerhubUserNameEnvVar(),
							dockerhubPasswordEnvVar(),
							dockerhubCredentialId())
				}
				timeout {
					noActivity(300)
					failBuild()
					writeDescription('Build failed due to timeout after {0} minutes of inactivity')
				}
			}
			steps {
				shell(buildWithDocs())
			}
			configure {
				SlackPlugin.slackNotification(it as Node) {
					room(cloudRoom())
				}
			}
			publishers {
				archiveJunit gradleJUnitResults()
			}
		}
	}

	private String buildWithDocs() {
		return """#!/bin/bash -x
					${setupGitCredentials()}
					${setOrigin()}
					${checkoutMaster()}
					(${build()} && ${syncDocs()} && ${cleanGitCredentials()}) || ${cleanGitCredentials()}
					${dockerBuildAndPush()}
					"""
	}

	private String setOrigin() {
		return "git remote set-url --push origin `git config remote.origin.url | sed -e 's/^git:/https:/'`"
	}

	private String checkoutMaster() {
		return "git checkout master && git pull origin master"
	}

	private String build() {
		return "./gradlew clean build generateReadme"
	}

	private String syncDocs() {
		return """git commit -a -m "Sync docs" && git push origin ${masterBranch()}"""
	}

	private String buildNumber() {
		return '${BUILD_NUMBER}'
	}

	private String dockerBuildAndPush() {
		return """
			echo "Deploying image to DockerHub"
			docker login --username=\$${dockerhubUserNameEnvVar()} --password=\$${dockerhubPasswordEnvVar()}
			echo "Docker images"
			docker images
			echo "Performing Docker Build"
			docker build -t springcloud/spring-cloud-pipeline-jenkins ./jenkins
			echo "Docker images post build"
			docker images
			echo "Pushing LATEST image to DockerHub"
			docker push springcloud/spring-cloud-pipeline-jenkins:latest
			echo "Removing all local images"
			docker rmi -f springcloud/spring-cloud-pipeline-jenkins
		"""
	}
}
