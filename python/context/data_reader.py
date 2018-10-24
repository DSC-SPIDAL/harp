class DataReader:

    def __init__(self, data_reader):
        self.dr = data_reader

    def next_key_val(self) -> bool:
        return self.dr.nextKeyValue()

    @property
    def current_key(self):
        return self.dr.getCurrentKey()

    @property
    def current_val(self):
        return self.dr.getCurrentValue()
