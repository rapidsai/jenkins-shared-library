final BRANCH_PR_TEST_STAGE = "branch_pr_test"
final NIGHTLY_TEST_STAGE = "nightly_test"
final CUDA_BUILD_STAGE = "cuda_build"
final PYTHON_BUILD_STAGE = "python_build"

def call(stage, Closure steps) {
  parallels_config = [
    "${BRANCH_PR_TEST_STAGE}": [
      [label: "driver-495-arm", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arch: "arm64"],
      [label: "driver-495-arm", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "arm64"],

      [label: "driver-450", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arch: "amd64"],
      [label: "driver-495", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arch: "amd64"],
      [label: "driver-495", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "amd64"],
    ],
    "${NIGHTLY_TEST_STAGE}": [
      [label: "driver-495-arm", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arch: "arm64"],
      [label: "driver-495-arm", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "arm64"],

      [label: "driver-450", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arch: "amd64"],
      [label: "driver-495", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arch: "amd64"],
      [label: "driver-495", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arch: "amd64"],

    ],
    "${CUDA_BUILD_STAGE}": [
        [arch: "arm64", label: "cpu4-arm64", os: "ubuntu18.04", cuda_ver: "11.5"],
        [arch: "amd64", label: "cpu4-amd64", os: "centos7", cuda_ver: "11.5"]
    ],
    "${PYTHON_BUILD_STAGE}": [
        [arch: "arm64", py_ver: "3.8", label: "cpu-arm64", cuda_ver: "11.5", os: "ubuntu18.04"],
        [arch: "arm64", py_ver: "3.9", label: "cpu-arm64", cuda_ver: "11.5", os: "ubuntu18.04"],
        [arch: "amd64", py_ver: "3.8", label: "cpu", cuda_ver: "11.5", os: "centos7"],
        [arch: "amd64", py_ver: "3.9", label: "cpu", cuda_ver: "11.5", os: "centos7"],
    ]
  ]
  return generateStage(stage, parallels_config, steps)
}


def generateTestStage(test_config, steps) {
  return {
    stage("Test - ${test_config.label} - ${test_config.cuda_ver} - ${test_config.py_ver} - ${test_config.os}") {
      node(test_config.label) {
        docker
          .image(getStageImg(test_config, false))
          .inside("""
            --runtime=nvidia
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
          runStepsWithNotify(steps, test_config, BRANCH_PR_TEST_STAGE)
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
    case BRANCH_PR_TEST_STAGE:
      return parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label} - ${it.cuda_ver} - ${it.py_ver} - ${it.os}" : generateTestStage(it, steps)]
      }
    case NIGHTLY_TEST_STAGE:
      return parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label} - ${it.cuda_ver} - ${it.py_ver} - ${it.os}" : generateNightlyTestStage(it, steps)]
      }
    case CUDA_BUILD_STAGE:
      return parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label}" : generateCudaBuildStage(it, steps)]
      }
    case PYTHON_BUILD_STAGE:
      return parallels_config[stage].collectEntries {
        ["${it.arch}: ${it.label} -${it.py_ver}" : generatePythonBuildStage(it, steps)]
      }
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

  return "gpuci/${img}:22.06-cuda${cuda_ver}-devel-${os}-py${py_ver}"
}

def runStepsWithNotify(Closure steps, test_config, String stage) {
  String ctx = generateContext(stage, test_config)

  try {
    githubNotify description: "Build ${BUILD_NUMBER} is now pending", status: 'PENDING', context: ctx, targetUrl: env.RUN_DISPLAY_URL
    steps()
    githubNotify description: "Build ${BUILD_NUMBER} succeeded in ${(currentBuild.durationString as Integer) / 60000} minutes", status: 'SUCCESS', context: ctx, targetUrl: env.RUN_DISPLAY_URL
  } catch (e) {
    githubNotify description: "Build${BUILD_NUMBER} failed in ${(currentBuild.durationString as Integer) / 60000} minutes", status: 'FAILURE', context: ctx, targetUrl: env.RUN_DISPLAY_URL
  }
}

def generateContext(String stage, test_config) {
  switch(stage) {
    case BRANCH_PR_TEST_STAGE:
      return "test/cuda/${test_config.arch}/${test_config.cuda_ver}/${test_config.label}/python/${test_config.py_ver}/${test_config.os}"
    case NIGHTLY_TEST_STAGE:
      return "test/cuda/${test_config.arch}/${test_config.cuda_ver}/${test_config.label}/python/${test_config.py_ver}/${test_config.os}"
    case CUDA_BUILD_STAGE:
      return "build/cuda/${test_config.arch}/${test_config.cuda_ver}"
    case PYTHON_BUILD_STAGE:
      return "build/python/${test_config.py_ver}/${test_config.cuda_ver}/cuda/${test_config.arch}/${test_config.cuda_ver}"
    default: throw new Exception("Invalid stage name provided")
  }
}
