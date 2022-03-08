def call(arc) {
    node {
        if(arc == "arm64") {
            return 'gpuci/rapidsai-arm64:22.04-cuda11.5-devel-ubuntu18.04-py3.8'
        } else {
            return 'gpuci/rapidsai:22.04-cuda11.5-devel-centos7-py3.8'
        }
    }
}