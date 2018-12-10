from harp.ml.MLKernels import MLKernels


class HarpSession:

    def __init__(self, java_session):
        self.__java_session = java_session
        self.ml = MLKernels(java_session.ml())

    @property
    def name(self):
        return self.__java_session.getName()
