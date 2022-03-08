def generateStage(test_config, steps) {
  return {
    stage("Test - ${test_config.label} - ${test_config.cuda_ver} - ${test_config.py_ver} - ${test_config.os}") {
      node(test_config.label) {
        docker.image("gpuci/rapidsai:22.04-cuda${test_config.cuda_ver}-devel-${test_config.os}-py${test_config.py_ver}").inside("--runtime=nvidia -e NVIDIA_VISIBLE_DEVICES=$EXECUTOR_NUMBER") {
          steps()
        }
      }
    }
  }
}

def call(Closure steps) {
  test_configs  = [
    [label: "driver-450", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arc: "arm64"],
    [label: "driver-495-arm", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arc: "arm64"],
    [label: "driver-495-arm", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arc: "arm64"],
    
    // repeat the same combinations for amd64 arc
    [label: "driver-450", cuda_ver: "11.0", py_ver: "3.8", os: "centos7", arc: "amd64"],
    [label: "driver-495", cuda_ver: "11.2", py_ver: "3.9", os: "ubuntu18.04", arc: "amd64"],
    [label: "driver-495", cuda_ver: "11.5", py_ver: "3.9", os: "ubuntu20.04", arc: "amd64"],
  ]
  node {
    return test_configs.collectEntries {
      ["${it.arc}: ${it.label} - ${it.cuda_ver} - ${it.py_ver} - ${it.os}" : generateStage(it, steps)]
    }
  }
}