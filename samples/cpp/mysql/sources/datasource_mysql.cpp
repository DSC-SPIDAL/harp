/* file: datasource_mysql.cpp */
/*******************************************************************************
* Copyright 2017 Intel Corporation
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

/*
! Content:
!
! C++ sample of a MySQL data source.
!
! 1) Connect to the database and create a table there.
!
! 2) Create a dictionary from the data table and read the data from it using
!    the ODBCDataSource functionality. Print the data from the table.
!
! 3) Delete the table from the database and disconnect.
!
!******************************************************************************/

#if defined(_WIN32) || defined(_WIN64)
#define NOMINMAX
#include <windows.h>
#endif

#include <sql.h>
#include <sqltypes.h>
#include <sqlext.h>
#include <iostream>
#include <ctime>

#include "daal.h"
#include "data_management/data_source/odbc_data_source.h"
#include "service.h"

using namespace daal;
using namespace std;

#if defined(_WIN64) || defined (__x86_64__)
string dataSourceName = "mySQL_test";
#else
string dataSourceName = "mySQL_test_32";
#endif

SQLRETURN connectServer();
SQLRETURN createUniqueTableName(string &tableName);
SQLRETURN createMySQLTable(string tableName);
SQLRETURN dropMySQLTable(string tableName);
SQLRETURN disconnectServer();
void      diagnoseError(string functionName, SQLHANDLE handle, SQLSMALLINT handleType);

/* External handles to connect to the database to manage a table for the sample */
SQLHENV   henv  = SQL_NULL_HENV;
SQLHDBC   hdbc  = SQL_NULL_HDBC;
SQLHSTMT  hstmt = SQL_NULL_HSTMT;
SQLRETURN ret;

int main(int argc, char *argv[])
{
    SQLRETURN ret;
    string tableName;

    /* Connect to the server via the ODBC API */
    ret = connectServer();
    if (!SQL_SUCCEEDED(ret)) { return ret; }

    /* Create a unique name for the data table */
    ret = createUniqueTableName(tableName);
    if (!SQL_SUCCEEDED(ret)) { return ret; }
    cout << "tableName = " << tableName << endl;

    /* Create the data table with the unique name */
    ret = createMySQLTable(tableName);
    if (!SQL_SUCCEEDED(ret)) { return ret; }

    /* Create ODBCDataSource to read from the MySQL database to HomogenNumericTable */
    ODBCDataSource<MySQLFeatureManager> dataSource(
        dataSourceName, tableName, "", "",
        DataSource::doAllocateNumericTable, DataSource::doDictionaryFromContext);

    /* Get the number of rows in the data table */
    size_t nRows = dataSource.getNumberOfAvailableRows();

    /* Load the number of rows from the data table */
    dataSource.loadDataBlock();

    /* Print the numeric table */
    printNumericTable(dataSource.getNumericTable());

    /* Free the connection handles to read the data from the data table in ODBCDataSource */
    dataSource.freeHandles();

    /* Delete the table from the data source */
    ret = dropMySQLTable(tableName);
    if (!SQL_SUCCEEDED(ret)) { return ret; }

    /* Disconnect from the server */
    ret = disconnectServer();
    if (!SQL_SUCCEEDED(ret)) { return ret; }

    return 0;
};

/* Connect to the server via the ODBC API */
SQLRETURN connectServer()
{
    ret = SQLAllocHandle(SQL_HANDLE_ENV, SQL_NULL_HANDLE, &henv);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLAllocHandle henv", henv, SQL_HANDLE_ENV); return ret; }

    ret = SQLSetEnvAttr(henv, SQL_ATTR_ODBC_VERSION, (SQLPOINTER) SQL_OV_ODBC3, SQL_IS_UINTEGER);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLSetEnvAttr", henv, SQL_HANDLE_ENV); return ret; }

    ret = SQLAllocHandle(SQL_HANDLE_DBC, henv, &hdbc);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLAllocHandle hdbc", henv, SQL_HANDLE_ENV); return ret; }

    ret = SQLConnect(hdbc, (SQLCHAR *)dataSourceName.c_str(), SQL_NTS, NULL, 0, NULL, 0);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLConnect", hdbc, SQL_HANDLE_DBC); return ret; }

    return SQL_SUCCESS;
}

/* Create a unique table name with a connection ID as a key */
SQLRETURN createUniqueTableName(string &uniqueTableName)
{
    double connectionID;
    string query_exec = "SELECT CONNECTION_ID();";
    SQLHSTMT hstmt = SQL_NULL_HSTMT;

    ret = SQLAllocHandle(SQL_HANDLE_STMT, hdbc, &hstmt);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLAllocHandle createName", hdbc, SQL_HANDLE_DBC); return ret; }

    ret = SQLExecDirect(hstmt, (SQLCHAR *)query_exec.c_str(), SQL_NTS);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLExecDirect createName", hstmt, SQL_HANDLE_STMT); return ret; }

    ret = SQLBindCol(hstmt, 1, SQL_C_DOUBLE, (SQLPOINTER)&connectionID, 0, NULL);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLBindCol createName", hstmt, SQL_HANDLE_STMT); return ret; }

    ret = SQLFetchScroll(hstmt, SQL_FETCH_NEXT, 1);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLFetchScroll createName", hstmt, SQL_HANDLE_STMT); return ret; }

    ret = SQLFreeHandle(SQL_HANDLE_STMT, hstmt);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLFreeHandle createName", hstmt, SQL_HANDLE_STMT); return ret; }

    time_t seconds = time(NULL);

    stringstream ss;
    ss << "daal_table" << "_" << connectionID << "_" << seconds;

    uniqueTableName = ss.str();

    return SQL_SUCCESS;
}

/* Create the data table with the unique table name */
SQLRETURN createMySQLTable(string tableName)
{
    SQLHSTMT hstmt = SQL_NULL_HSTMT;
    ret = SQLAllocHandle(SQL_HANDLE_STMT, hdbc, &hstmt);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLAllocHandle create", hdbc, SQL_HANDLE_DBC); return ret; }

    string createTableSQLStatement = "CREATE TABLE " + tableName + " (DoubleColumn1 double, DoubleColumn2 double);";
    string insertSQLStatement      = "INSERT INTO " + tableName + " VALUES (1.23, 4.56), (7.89, 1.56), (2.62, 9.35);";

    ret = SQLExecDirect(hstmt, (SQLCHAR *)createTableSQLStatement.c_str(), SQL_NTS);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLExecDirect create", hstmt, SQL_HANDLE_STMT); return ret; }

    ret = SQLExecDirect(hstmt, (SQLCHAR *)insertSQLStatement.c_str(), SQL_NTS);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLExecDirect insert", hstmt, SQL_HANDLE_STMT); return ret; }

    ret = SQLFreeHandle(SQL_HANDLE_STMT, hstmt);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLFreeHandle create", hstmt, SQL_HANDLE_STMT); return ret; }

    return SQL_SUCCESS;
}

/* Drop the table with the unique table name */
SQLRETURN dropMySQLTable(string tableName)
{
    string dropTableSQLStatement = "DROP TABLE " + tableName + ";";
    SQLHSTMT hstmt = SQL_NULL_HSTMT;

    ret = SQLAllocHandle(SQL_HANDLE_STMT, hdbc, &hstmt);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLAllocHandle drop", hdbc, SQL_HANDLE_DBC); return ret; }

    ret = SQLExecDirect(hstmt, (SQLCHAR *)dropTableSQLStatement.c_str(), SQL_NTS);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLExecDirect drop", hstmt, SQL_HANDLE_STMT); return ret; }

    ret = SQLFreeHandle(SQL_HANDLE_STMT, hstmt);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLFreeHandle drop", hstmt, SQL_HANDLE_STMT); return ret; }

    return SQL_SUCCESS;
}

/* Disconnect from the server via the ODBC API */
SQLRETURN disconnectServer()
{
    ret = SQLDisconnect(hdbc);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLDisconnect", hdbc, SQL_HANDLE_DBC); return ret; }

    ret = SQLFreeHandle(SQL_HANDLE_DBC, hdbc);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLFreeHandle hdbc disconnect", hdbc, SQL_HANDLE_DBC); return ret; }

    ret = SQLFreeHandle(SQL_HANDLE_ENV, henv);
    if (!SQL_SUCCEEDED(ret)) { diagnoseError("SQLFreeHandle henv disconnect", henv, SQL_HANDLE_ENV); return ret; }

    return SQL_SUCCESS;
}

/* Diagnose an ODBC error */
void diagnoseError(string functionName, SQLHANDLE handle, SQLSMALLINT handleType)
{
    SQLINTEGER nativeError;
    SQLCHAR SQLState[6], messageText[SQL_MAX_MESSAGE_LENGTH];
    SQLSMALLINT i, textLength;
    SQLRETURN ret = SQL_SUCCESS;
    cout << endl << "The driver returns diagnostic message from function " << functionName << endl << endl;

    i = 1;
    while ( ret != SQL_NO_DATA )
    {
        ret = SQLGetDiagRec(handleType, handle, i++, SQLState, &nativeError, messageText, sizeof(messageText), &textLength);
        if (SQL_SUCCEEDED(ret))
        {
            cout << SQLState << ":" << i << ":" << nativeError << ":" << messageText << endl;
        }
    }
}

/* Check the status. In case of failure, drop the table, disconnect, and exit with an error code */
void free(string tableName, string functionName)
{
    cout << "Error in function:" << functionName << endl;
    dropMySQLTable(tableName);
    disconnectServer();
}
