pipelineJob('Branch-aduprat-Jenkins2') {
  definition {
    cpsScm {
      scm {
        github('aduprat/james-project', 'jenkins2-mr')
      }
      scriptPath('ci/JenkinsfileBranch')
    }
  }
  triggers {
    githubPush()
  }
}
