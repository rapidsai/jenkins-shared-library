import groovy.transform.Field


@Field final PR_TEST_STAGE = "pr_test"
@Field final NIGHTLY_TEST_STAGE = "nightly_test"
@Field final CUDA_BUILD_STAGE = "cuda_build"
@Field final PYTHON_BUILD_STAGE = "python_build"
@Field final WSL2_NIGHTLY_TEST_STAGE = "wsl2_nightly_test"

def call(stage, Closure steps) {
  parallels_config = [
    pr_test: [
      [label: "driver-495-arm", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "arm64"],

      [label: "driver-450", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arch: "amd64"],
      [label: "driver-495", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arch: "amd64"],
      [label: "driver-495", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "amd64"],
    ],
    nightly_test: [
      [label: "driver-495-arm", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "arm64"],

      [label: "driver-450", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arch: "amd64"],
      [label: "driver-495", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arch: "amd64"],
      [label: "driver-495", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "amd64"],

    ],
    wsl2_nightly_test: [
      // [label: "wsl2", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "arm64"],

      [label: "wsl2", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arch: "amd64"],
      [label: "wsl2", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arch: "amd64"],
      [label: "wsl2", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "amd64"],
    ],
    cuda_build: [
        [arch: "arm64", label: "cpu4-arm64", os: "ubuntu18.04", cuda_ver: "11.5"],
        [arch: "amd64", label: "cpu4-amd64", os: "centos7", cuda_ver: "11.5"]
    ],
    python_build: [
        [arch: "arm64", py_ver: "3.8", label: "cpu-arm64", cuda_ver: "11.5", os: "ubuntu18.04"],
        [arch: "arm64", py_ver: "3.9", label: "cpu-arm64", cuda_ver: "11.5", os: "ubuntu18.04"],
        [arch: "amd64", py_ver: "3.8", label: "cpu", cuda_ver: "11.5", os: "centos7"],
        [arch: "amd64", py_ver: "3.9", label: "cpu", cuda_ver: "11.5", os: "centos7"],
    ]
  ]
  return generateStage(stage, parallels_config, steps)
}


def generateWSL2NightlyTestStage(test_config, steps) {
  return {
      stage("WSL2 Nightly Test - ${test_config.label} - ${test_config.cuda_ver} - ${test_config.py_ver} - ${test_config.os}") {
        node(test_config.label) {
          docker
            .image(getStageImg(test_config, false))
            .inside("""
              -e NVIDIA_VISIBLE_DEVICES=$EXECUTOR_NUMBER
              -e ARCH=${test_config.arch}
              -e CUDA=${test_config.cuda_ver}
              -e PY_VER=${test_config.py_ver}
              -e HOME=$WORKSPACE
            """) {
          cleanWs (
            deleteDirs: true,
            externalDelete: 'sudo rm -rf %s'
          )
          checkout scm
          runStepsWithNotify(steps, test_config, WSL2_NIGHTLY_TEST_STAGE)
        }
      }
    }
  }
}


def generateTestStage(test_config, steps) {
  return {
    stage("Test - ${test_config.label} - ${test_config.cuda_ver} - ${test_config.py_ver} - ${test_config.os}") {
      node(test_config.label) {
        docker
          .image(getStageImg(test_config, false))
          .inside("""
            --runtime=nvidia
            --pull=always
            -e NVIDIA_VISIBLE_DEVICES=$EXECUTOR_NUMBER
            -e ARCH=${test_config.arch}
            -e CUDA=${test_config.cuda_ver}
            -e PY_VER=${test_config.py_ver}
            -e HOME=$WORKSPACE
          """) {
          cleanWs (
            deleteDirs: true,
            externalDelete: 'sudo rm -rf %s'
          )
          checkout scm
          runStepsWithNotify(steps, test_config, PR_TEST_STAGE)
        }
      }
    }
  }
}

def generateNightlyTestStage(test_config, steps) {
    return {
      stage("Nightly Test - ${test_config.label} - ${test_config.cuda_ver} - ${test_config.py_ver} - ${test_config.os}") {
        node(test_config.label) {
          docker
            .image(getStageImg(test_config, false))
            .inside("""
              --runtime=nvidia
              --pull=always
              -e NVIDIA_VISIBLE_DEVICES=$EXECUTOR_NUMBER
              -e ARCH=${test_config.arch}
              -e CUDA=${test_config.cuda_ver}
              -e PY_VER=${test_config.py_ver}
              -e HOME=$WORKSPACE
            """) {
          cleanWs (
            deleteDirs: true,
            externalDelete: 'sudo rm -rf %s'
          )
          checkout scm
          runStepsWithNotify(steps, test_config, NIGHTLY_TEST_STAGE)
        }
      }
    }
  }
}

def generateCudaBuildStage(test_config, steps) {
  return {
    stage("C++ build - ${test_config.label}") {
      node(test_config.label) {
        docker
          .image(getStageImg(test_config, true))
          .inside("""
            --pull=always
            -e ARCH=${test_config.arch}
            -e CUDA=${test_config.cuda_ver}
            -e HOME=$WORKSPACE
          """) {
          cleanWs (
            deleteDirs: true,
            externalDelete: 'sudo rm -rf %s'
          )
          checkout scm
          runStepsWithNotify(steps, test_config, CUDA_BUILD_STAGE)
        }
      }
    }
  }
}

def generatePythonBuildStage(test_config, steps) {
  return {
    stage("Python build - ${test_config.label}") {
      node(test_config.label) {
        docker
          .image(getStageImg(test_config, true))
          .inside("""
            --pull=always
            -e ARCH=${test_config.arch}
            -e PY_VER=${test_config.py_ver}
            -e CUDA=${test_config.cuda_ver}
            -e HOME=$WORKSPACE
          """) {
          cleanWs (
            deleteDirs: true,
            externalDelete: 'sudo rm -rf %s'
          )
          checkout scm
          runStepsWithNotify(steps, test_config, PYTHON_BUILD_STAGE)
        }
      }
    }
  }
}

def generateStage(stage, parallels_config, steps) {
  switch(stage) {
    case PR_TEST_STAGE:
      def stages = parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label} - ${it.cuda_ver} - ${it.py_ver} - ${it.os}" : generateTestStage(it, steps)]
      }
      stages.failFast = true
      return stages
    case NIGHTLY_TEST_STAGE:
      def stages = parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label} - ${it.cuda_ver} - ${it.py_ver} - ${it.os}" : generateNightlyTestStage(it, steps)]
      }
      stages.failFast = true
      return stages
    case CUDA_BUILD_STAGE:
      def stages = parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label} - ${it.os} - ${it.cuda_ver}" : generateCudaBuildStage(it, steps)]
      }
      stages.failFast = true
      return stages
    case PYTHON_BUILD_STAGE:
      def stages = parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label} - ${it.py_ver} - ${it.os} - ${it.cuda_ver}" : generatePythonBuildStage(it, steps)]
      }
      stages.failFast = true
      return stages
    case WSL2_NIGHTLY_TEST_STAGE:
      def stages = parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label} - ${it.cuda_ver} - ${it.py_ver} - ${it.os}" : generateWSL2NightlyTestStage(it, steps)]
      }
      stages.failFast = true
      return stages
    default: throw new Exception("Invalid stage name provided")
  }
}

def getStageImg(config, is_build_stage) {
  String img = "rapidsai"
  String os = config.os
  String cuda_ver = config.cuda_ver
  String py_ver = config.getOrDefault("py_ver", "3.9") // CUDA builds don't require specific Python version, so default to an arbitrary version

  if (is_build_stage) {
    img += "-driver"
  }

  if (config.arch == "arm64") {
    img += "-arm64"
  }

  return "gpuci/${img}:22.08-cuda${cuda_ver}-devel-${os}-py${py_ver}"
}

def runStepsWithNotify(Closure steps, test_config, String stage) {
  String ctx = generateContext(stage, test_config)

  try {
    githubNotify description: "Build ${BUILD_NUMBER} is now pending", status: 'PENDING', context: ctx, targetUrl: env.RUN_DISPLAY_URL
    withCredentials([[
      $class: 'AmazonWebServicesCredentialsBinding',
      credentialsId: "aws-s3-gpuci",
      accessKeyVariable: 'AWS_ACCESS_KEY_ID',
      secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
    ]]) {
      steps()
    }
    githubNotify description: "Build ${BUILD_NUMBER} succeeded", status: 'SUCCESS', context: ctx, targetUrl: env.RUN_DISPLAY_URL
  } catch (e) {
    githubNotify description: "Build ${BUILD_NUMBER} failed", status: 'FAILURE', context: ctx, targetUrl: env.RUN_DISPLAY_URL
    error e
  }
}

def generateContext(String stage, test_config) {
  switch(stage) {
    case PR_TEST_STAGE:
      return "test/cuda/${test_config.arch}/${test_config.cuda_ver}/${test_config.label}/python/${test_config.py_ver}/${test_config.os}"
    case NIGHTLY_TEST_STAGE:
      return "test/cuda/${test_config.arch}/${test_config.cuda_ver}/${test_config.label}/python/${test_config.py_ver}/${test_config.os}"
    case CUDA_BUILD_STAGE:
      return "build/cuda/${test_config.arch}/${test_config.cuda_ver}"
    case PYTHON_BUILD_STAGE:
      return "build/python/${test_config.py_ver}/${test_config.cuda_ver}/cuda/${test_config.arch}/${test_config.cuda_ver}"
    case WSL2_NIGHTLY_TEST_STAGE:
      return "wsl2_test/cuda/${test_config.arch}/${test_config.cuda_ver}/${test_config.label}/python/${test_config.py_ver}/${test_config.os}"
    default: throw new Exception("Invalid stage name provided")
  }
}
