package edu.iu.harp.boot.python.io.util;

import edu.iu.harp.boot.python.HarpSession;
import edu.iu.harp.boot.python.io.filepointers.AbstractFilePointer;
import edu.iu.harp.boot.python.io.filepointers.HDFSFilePointer;
import edu.iu.harp.boot.python.io.filepointers.LocalFilePointer;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileSynchronizer {

    private final HarpSession harpSession;
    private LocalFileSystem localFs;
    private FileSystem hdfs;

    public FileSynchronizer(HarpSession harpSession, LocalFileSystem localFs, FileSystem hdfs) {
        this.localFs = localFs;
        this.hdfs = hdfs;
        this.harpSession = harpSession;
    }

    public List<HDFSFilePointer> syncToHdfs(List<LocalFilePointer> localFiles) throws IOException {
        List<HDFSFilePointer> hdfsFilePointers = new ArrayList<>();
        for (LocalFilePointer localFilePointer : localFiles) {
            if (!localFs.exists(localFilePointer.getPath())) {
                throw new FileNotFoundException(localFilePointer.getPath() + " not found");
            } else {
                String newHdfsFilePath = String.format("/harp/%s/%s", harpSession.getName(), localFilePointer.getPath().getName());
                HDFSFilePointer hdfsFilePointer = new HDFSFilePointer(newHdfsFilePath);
                if (hdfs.exists(hdfsFilePointer.getPath())) {
                    System.out.println("File already exists in hadoop. Checking checksum...");
                    if (hdfs.getFileChecksum(hdfsFilePointer.getPath()).equals(localFs.getFileChecksum(localFilePointer.getPath()))) {
                        System.out.println("Same file exists in hdfs. Not uploading");
                    } else {
                        writeToHdfs(localFilePointer, hdfsFilePointer);
                    }
                } else {
                    System.out.println("Writing to hdfs");
                    this.writeToHdfs(localFilePointer, hdfsFilePointer);
                }
                hdfsFilePointers.add(hdfsFilePointer);
            }
        }
        return hdfsFilePointers;
    }

    private void writeToHdfs(LocalFilePointer localFilePointer, HDFSFilePointer hdfsFilePointer) throws IOException {
        hdfs.mkdirs(hdfsFilePointer.getPath().getParent());
        hdfs.copyFromLocalFile(localFilePointer.getPath(), hdfsFilePointer.getPath());
    }
}
