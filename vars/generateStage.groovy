def generateTestStage(test_config, steps) {
  return {
    stage("Test - ${test_config.label} - ${test_config.cuda_ver} - ${test_config.py_ver} - ${test_config.os}") {
      node(test_config.label) {
        docker
          .image("gpuci/${getArcImageString(test_config.arc)}:22.06-cuda${test_config.cuda_ver}-devel-${test_config.os}-py${test_config.py_ver}")
          .inside("--runtime=nvidia -e NVIDIA_VISIBLE_DEVICES=$EXECUTOR_NUMBER -e ARC=${test_config.arc} -e CUDA=${test_config.cuda_ver} -e PY_VER=${test_config.py_ver}") {
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
                  .image("gpuci/${getArcImageString(test_config.arc)}:22.06-cuda${test_config.cuda_ver}-devel-${test_config.os}-py${test_config.py_ver}")
                  .inside("--runtime=nvidia -e NVIDIA_VISIBLE_DEVICES=$EXECUTOR_NUMBER -e ARC=${test_config.arc} -e CUDA=${test_config.cuda_ver} -e PY_VER=${test_config.py_ver}") {
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
                  .image("${getArcImage(test_config.arc)}")
                  .inside("-e ARC=${test_config.arc}") {
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
                  .image("gpuci/${getArcImageStringForPyBuild(test_config.arc)}:22.06-cuda${test_config.cuda_ver}-devel-${test_config.os}-py${test_config.py_ver}")
                  .inside("-e ARC=${test_config.arc} -e PY_VER=${test_config.py_ver}") {
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
          [arc: "arm64", label: "cpu4-arm64"],
          [arc: "amd64", label: "cpu4-amd64"]
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

def getArcImageString(arc) {
  if(arc == "arm64") {
      return 'rapidsai-arm64'
  } else {
      return 'rapidsai'
  }
}


def getArcImageStringForPyBuild(arc) {
  if(arc == "arm64") {
      return 'rapidsai-driver-arm64'
  } else {
      return 'rapidsai'
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

def getArcImage(arc) {
    if(arc == "arm64") {
            return 'gpuci/rapidsai-arm64:22.06-cuda11.5-devel-ubuntu18.04-py3.8'
    } else {
        return 'gpuci/rapidsai:22.06-cuda11.5-devel-centos7-py3.8'
    }
}
