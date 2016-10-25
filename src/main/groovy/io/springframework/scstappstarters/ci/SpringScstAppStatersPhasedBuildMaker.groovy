package io.springframework.scstappstarters.ci

import io.springframework.scstappstarters.common.AllScstAppStarterJobs
import io.springframework.scstappstarters.common.SpringScstAppStarterJobs
import javaposse.jobdsl.dsl.DslFactory
/**
 * @author Soby Chacko
 */
class SpringScstAppStatersPhasedBuildMaker implements SpringScstAppStarterJobs {

    private final DslFactory dsl

    final String branchToBuild = "master"
    final String coreProject = "core"

    SpringScstAppStatersPhasedBuildMaker(DslFactory dsl) {
        this.dsl = dsl
    }

    void build() {
        buildAllRelatedJobs()
        dsl.multiJob("spring-scst-app-starter-builds") {
            steps {
                int counter = 1
                (AllScstAppStarterJobs.PHASES).each { List<String> ph ->
                    phase("phase-${counter}-job") {
                        ph.each {
                            String projectName ->
                            String prefixedProjectName = prefixJob(projectName)
                            phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
                                currentJobParameters()
                            }
                        }
                    }
                    counter++;
                }



//                phase('phase-1-jobs') {
//                    triggers {
//                        githubPush()
//                    }
//                    scm {
//                        git {
//                            remote {
//                                url "https://github.com/spring-cloud-stream-app-starters/core"
//                                branch branchToBuild
//                            }
//                        }
//                    }
//                    (AllScstAppStarterJobs.CORE_PHASE).each { String projectName ->
//                        String prefixedProjectName = prefixJob(projectName)
//                        phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
//                            currentJobParameters()
//                        }
//                    }
//                }
//                phase('phase-2-jobs') {
//                    (AllScstAppStarterJobs.PHASE1_JOBS).each { String projectName ->
//                        String prefixedProjectName = prefixJob(projectName)
//                        phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
//                            currentJobParameters()
//                        }
//                    }
//                }
//                phase('phase-3-jobs') {
//                    (AllScstAppStarterJobs.PHASE2_JOBS).each { String projectName ->
//                        String prefixedProjectName = prefixJob(projectName)
//                        phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
//                            currentJobParameters()
//                        }
//                    }
//                }
            }
        }
    }

    void buildAllRelatedJobs() {
        new SpringScstAppStartersBuildMaker(dsl, "spring-cloud-stream-app-starters", "core").deployNonAppStarters()
        AllScstAppStarterJobs.ALL_JOBS.each {
            new SpringScstAppStartersBuildMaker(dsl, "spring-cloud-stream-app-starters", it).deploy()
        }
    }

}