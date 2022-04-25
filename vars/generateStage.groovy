def generateTestStage(test_config, steps) {
  return {
    stage("Test - ${test_config.label} - ${test_config.cuda_ver} - ${test_config.py_ver} - ${test_config.os}") {
      node(test_config.label) {
        docker
          .image(getStageImg(test_config, false))
          .inside("""
            --runtime=nvidia
            -e NVIDIA_VISIBLE_DEVICES=$EXECUTOR_NUMBER
            -e ARC=${test_config.arc}
            -e CUDA=${test_config.cuda_ver}
            -e PY_VER=${test_config.py_ver}
            -e HOME=$WORKSPACE
          """) {
          steps()
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
                    -e ARC=${test_config.arc}
                    -e CUDA=${test_config.cuda_ver}
                    -e PY_VER=${test_config.py_ver}
                    -e HOME=$WORKSPACE
                  """) {
                steps()
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
                    -e ARC=${test_config.arc}
                  """) {
                  steps()
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
                    -e ARC=${test_config.arc}
                    -e PY_VER=${test_config.py_ver}
                  """) {
                  steps()
                }
            }
        }
    }
}

def call(stage, Closure steps) {
  parallels_config = [
      branch_pr_test: [
        // [label: "driver-495-arm", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arc: "arm64"],
        // [label: "driver-495-arm", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arc: "arm64"],

        // [label: "driver-450", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arc: "amd64"],
        // [label: "driver-495", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arc: "amd64"],
        [label: "driver-495", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arc: "amd64"],
      ],
      nightly_test: [
        [label: "driver-495-arm", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arc: "arm64"],
        [label: "driver-495-arm", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arc: "arm64"],

        [label: "driver-450", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arc: "amd64"],
        [label: "driver-495", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arc: "amd64"],
        [label: "driver-495", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arc: "amd64"],

      ],
      cuda_build: [
          [arc: "arm64", label: "cpu4-arm64", os: "centos7", cuda_ver: "11.5"],
          [arc: "amd64", label: "cpu4-amd64", os: "centos7", cuda_ver: "11.5"]
      ],
      python_build: [
          [arc: "arm64", py_ver: "3.8", label: "cpu-arm64", cuda_ver: "11.5", os: "ubuntu18.04"],
          [arc: "arm64", py_ver: "3.9", label: "cpu-arm64", cuda_ver: "11.5", os: "ubuntu18.04"],
          [arc: "amd64", py_ver: "3.8", label: "cpu", cuda_ver: "11.5", os: "centos7"],
          [arc: "amd64", py_ver: "3.9", label: "cpu", cuda_ver: "11.5", os: "centos7"],
      ]
  ]
  node {
    return generateStage(stage, parallels_config, steps)
  }
}

def generateStage(stage, parallels_config, steps) {
  switch(stage) {
    case "branch_pr_test":
        return parallels_config[stage].collectEntries {
            ["${it.arc}: ${it.label} - ${it.cuda_ver} - ${it.py_ver} - ${it.os}" : generateTestStage(it, steps)]
        }
    case "nightly_test":
      return parallels_config[stage].collectEntries {
            ["${it.arc}: ${it.label} - ${it.cuda_ver} - ${it.py_ver} - ${it.os}" : generateNightlyTestStage(it, steps)]
        }
    case "cuda_build":
      return parallels_config[stage].collectEntries {
            ["${it.arc}: ${it.label}" : generateCudaBuildStage(it, steps)]
        }
    case "python_build":
      return parallels_config[stage].collectEntries {
            ["${it.arc}: ${it.label} -${it.py_ver}" : generatePythonBuildStage(it, steps)]
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

  if (config.arc === "arm64") {
    img += "-arm64"
  }

  return "gpuci/${img}:22.06-cuda${cuda_ver}-devel-${os}-py${py_ver}"
}
