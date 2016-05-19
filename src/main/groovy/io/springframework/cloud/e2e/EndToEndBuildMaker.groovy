package io.springframework.cloud.e2e

import io.springframework.common.JdkConfig
import io.springframework.common.Label
import io.springframework.common.Notification
import io.springframework.common.Publisher
import javaposse.jobdsl.dsl.DslFactory

/**
 * @author Marcin Grzejszczak
 */
class EndToEndBuildMaker implements Notification, Publisher, JdkConfig, BreweryDefatuts, Label {

	private final DslFactory dsl

	EndToEndBuildMaker(DslFactory dsl) {
		this.dsl = dsl
	}

	void build(String projectName, String cronExpr) {
		build(projectName, "runAcceptanceTests", cronExpr)
	}

	protected void build(String projectName, String repoName, String scriptName, String cronExpr) {
		dsl.job("${projectName}-e2e") {
			triggers {
				cron cronExpr
			}
			jdk jdk8()
			wrappers {
				label aws()
				environmentVariables([
						RETRIES: 70,
						(jdk8HomeEnvVar()): jdk8DefaultPath()
				])
			}
			scm {
				git {
					remote {
						url "https://github.com/spring-cloud/$repoName"
						branch 'master'
					}

				}
			}
			steps {
				shell('''
						echo "Clearing mvn and gradle repos"
						rm -rf ~/.m2/repository/org/springframework/cloud/
						rm -rf ~/.gradle/caches/modules-2/files-2.1/org.springframework.cloud/
					''')
				shell("""
						sh -e scripts/${scriptName}.sh
					""")
			}
			configure {
				appendSlackNotificationForSpringCloud(it as Node)
			}
			publishers {
				archiveJunit gradleJUnitResults()
				archiveArtifacts acceptanceTestReports()
				archiveArtifacts acceptanceTestSpockReports()
			}
		}
	}

	protected void build(String projectName, String scriptName, String cronExpr) {
		build(projectName, projectName, scriptName, cronExpr)
	}

}
