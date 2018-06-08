/* file: hdfs_wrappers.h */
/*******************************************************************************
* Copyright 2014-2018 Intel Corporation
* All Rights Reserved.
*
* If this  software was obtained  under the  Intel Simplified  Software License,
* the following terms apply:
*
* The source code,  information  and material  ("Material") contained  herein is
* owned by Intel Corporation or its  suppliers or licensors,  and  title to such
* Material remains with Intel  Corporation or its  suppliers or  licensors.  The
* Material  contains  proprietary  information  of  Intel or  its suppliers  and
* licensors.  The Material is protected by  worldwide copyright  laws and treaty
* provisions.  No part  of  the  Material   may  be  used,  copied,  reproduced,
* modified, published,  uploaded, posted, transmitted,  distributed or disclosed
* in any way without Intel's prior express written permission.  No license under
* any patent,  copyright or other  intellectual property rights  in the Material
* is granted to  or  conferred  upon  you,  either   expressly,  by implication,
* inducement,  estoppel  or  otherwise.  Any  license   under such  intellectual
* property rights must be express and approved by Intel in writing.
*
* Unless otherwise agreed by Intel in writing,  you may not remove or alter this
* notice or  any  other  notice   embedded  in  Materials  by  Intel  or Intel's
* suppliers or licensors in any way.
*
*
* If this  software  was obtained  under the  Apache License,  Version  2.0 (the
* "License"), the following terms apply:
*
* You may  not use this  file except  in compliance  with  the License.  You may
* obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
*
* Unless  required  by   applicable  law  or  agreed  to  in  writing,  software
* distributed under the License  is distributed  on an  "AS IS"  BASIS,  WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*
* See the   License  for the   specific  language   governing   permissions  and
* limitations under the License.
*******************************************************************************/

#ifndef __DATA_MANAGEMENT_DATA_SOURCE_INTERNAL_HDFS_WRAPPERS_H__
#define __DATA_MANAGEMENT_DATA_SOURCE_INTERNAL_HDFS_WRAPPERS_H__

#include <hdfs.h>

#include "services/error_handling.h"
#include "services/daal_shared_ptr.h"
#include "services/internal/buffer.h"

namespace daal
{
namespace data_management
{
namespace internal
{

class HDFSFile;
// class HDFSConnection;

class HDFSConnection : public Base
{
public:
    class Impl : public Base
    {
    public:
        explicit Impl(const std::string &nameNode, uint16_t port,
                      services::Status *status = NULL)
        {
            _handle = hdfsConnect(nameNode.c_str(), (tPort)port);
            if (!_handle)
            {

            }
        }

        explicit Impl(const std::string &nameNode, uint16_t port, const std::string &user,
                      services::Status *status = NULL)
        {
            _handle = hdfsConnectAsUser(nameNode.c_str(), (tPort)port, user.c_str());
            if (!_handle)
            {

            }
        }

        ~Impl()
        {
            if (_handle)
            {
                hdfsDisconnect(_handle);
                _handle = NULL;
            }
        }

        hdfsFS native() const
        {
            return _handle;
        }

    private:
        /* Disable copy & assigment */
        Impl(const Impl &);
        Impl &operator = (const Impl &);

        hdfsFS _handle;
    };

    explicit HDFSConnection(services::Status *status = NULL)
    {
        _impl = services::SharedPtr<HDFSConnection::Impl>(
            new HDFSConnection::Impl("default", 0, status)
        );
        if (!_impl)
        {

        }
    }

    explicit HDFSConnection(const std::string &nameNode, uint16_t port,
                            services::Status *status = NULL)
    {
        _impl = services::SharedPtr<HDFSConnection::Impl>(
            new HDFSConnection::Impl(nameNode, port, status)
        );
        if (!_impl)
        {

        }
    }

    explicit HDFSConnection(const std::string &nameNode, uint16_t port, const std::string &user,
                            services::Status *status = NULL)
    {
        _impl = services::SharedPtr<HDFSConnection::Impl>(
            new HDFSConnection::Impl(nameNode, port, user, status)
        );
        if (!_impl)
        {

        }
    }

    hdfsFS native() const
    {
        return _impl->native();
    }

    inline HDFSFile openFile(const std::string &path,
                      services::Status *status = NULL);

private:
    services::SharedPtr<Impl> _impl;
};


class HDFSFile : public Base
{
public:
    class Impl : public Base
    {
    public:
        explicit Impl(const HDFSConnection &connection,
                      const std::string &path,
                      int flags,
                      size_t bufferSize,
                      bool replication,
                      size_t blockSize,
                      services::Status *status = NULL) :
            _connection(connection)
        {
            _handle = hdfsOpenFile(connection.native(), path.c_str(), flags,
                                   (int)bufferSize, (short)replication, blockSize);
            if (!_handle)
            {

            }
        }

        ~Impl()
        {
            if (_handle)
            {
                hdfsCloseFile(_connection.native(), _handle);
                _handle = NULL;
            }
        }

        hdfsFile native() const
        {
            return _handle;
        }

        uint32_t read(void *buffer, uint32_t length,
                      services::Status *status = NULL)
        {
            return hdfsRead(_connection.native(), _handle, buffer, length);
        }

        void seek(size_t desiredPos,
                  services::Status *status = NULL)
        {
            int code = hdfsSeek(_connection.native(), _handle, (tOffset)desiredPos);
        }

    private:
        /* Disable copy & assigment */
        Impl(const Impl &);
        Impl &operator = (const Impl &);

        hdfsFile _handle;
        HDFSConnection _connection;
    };

    HDFSFile() { }

    explicit HDFSFile(const HDFSConnection &connection,
                      const std::string &path,
                      int flags = O_RDONLY,
                      size_t bufferSize = 0,
                      bool replication = 0,
                      size_t blockSize = 0,
                      services::Status *status = NULL)
    {
        _impl = services::SharedPtr<HDFSFile::Impl>(
            new HDFSFile::Impl(connection, path, flags, bufferSize,
                               replication, blockSize, status)
        );
        if (!_impl)
        {

        }
    }

    hdfsFile native() const
    {
        if (!isOpen())
        { return NULL; }

        return _impl->native();
    }

    uint32_t read(void *buffer, uint32_t length,
                  services::Status *status = NULL)
    {
        return _impl->read(buffer, length, status);
    }

    uint32_t read(services::internal::Buffer<char> &buffer,
                  services::Status *status = NULL)
    {
        return _impl->read(buffer.data(), buffer.size(), status);
    }

    void seek(size_t desiredPos,
              services::Status *status = NULL)
    {
        return _impl->seek(desiredPos, status);
    }

    bool isOpen() const
    {
        return (bool)_impl;
    }

private:
    services::SharedPtr<Impl> _impl;
};

inline HDFSFile HDFSConnection::openFile(const std::string &path,
                                  services::Status *status)
{
    return HDFSFile(*this, path);
}

class HDSFFileReader : public Base
{
public:
    HDSFFileReader(services::Status *status = NULL) :
        _eofFlag(false),
        _fileBufferPos(0),
        _fileBufferUsedSize(0) { }

    explicit HDSFFileReader(const HDFSFile &file,
                            services::Status *status = NULL) :
        _file(file),
        _fileBuffer(FILE_BUFFER_SIZE, status),
        _eofFlag(false),
        _fileBufferPos(0),
        _fileBufferUsedSize(0) { }

    services::Status initialize(const HDFSFile &file)
    {
        _file = file;
        _eofFlag = false;
        _fileBufferPos = 0;
        _fileBufferUsedSize = 0;

        return _fileBuffer.reallocate(FILE_BUFFER_SIZE);
    }

    services::Status reset()
    {
        services::Status status;

        _file.seek(0, &status);
        _eofFlag = false;
        _fileBufferPos = 0;
        _fileBufferUsedSize = 0;

        return status;
    }

    bool eof() const
    {
        return _eofFlag;
    }

    uint32_t readLine(char *buffer, uint32_t bufferSize,
                      bool &endOfLineReached, services::Status *status = NULL)
    {
        DAAL_ASSERT( buffer );
        DAAL_ASSERT( _file.isOpen() );
        DAAL_ASSERT( _fileBuffer.size() == FILE_BUFFER_SIZE );

        uint32_t lineLength = 0;
        endOfLineReached = false;

        while (lineLength <= bufferSize)
        {
            if (_fileBufferPos >= _fileBufferUsedSize)
            {
                _fileBufferPos = 0;
                _fileBufferUsedSize = _file.read(_fileBuffer, status);

                /* We reached end-of-file */
                if (!_fileBufferUsedSize)
                {
                    _eofFlag = true;
                    endOfLineReached = true;
                    break;
                }

                continue;
            }

            buffer[ lineLength++ ] = _fileBuffer[ _fileBufferPos++ ];

            const char c = buffer[lineLength - 1];
            if (c == '\n' || c == '\0')
            { endOfLineReached = true; break; }
        }

        if (endOfLineReached && lineLength > 0)
        {
            if (buffer[lineLength - 1] == '\n')
            {
                lineLength--;
                buffer[lineLength] = '\0';
            }

            if (buffer[lineLength - 1] == '\r')
            {
                lineLength--;
                buffer[lineLength] = '\0';
            }
        }

        return lineLength;
    }

private:
    HDFSFile _file;
    services::internal::Buffer<char> _fileBuffer;

    bool _eofFlag;
    uint32_t _fileBufferPos;
    uint32_t _fileBufferUsedSize;

private:
    static const uint32_t FILE_BUFFER_SIZE = 1048576;
};

} // namespace internal
} // namespace data_management
} // namespace daal

#endif
