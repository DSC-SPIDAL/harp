package edu.iu.harp.io;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

public class ConnPoolTest {
  @Test
  public void testGetConn() throws Exception {
//    Connection connection = PowerMockito.mock(Connection.class);
//    PowerMockito.whenNew(Connection.class).withArguments("localhost", 10045, 0, true).
//        thenReturn(connection);
//    PowerMockito.when(connection.getPort()).thenReturn(10045);
//    PowerMockito.when(connection.getNode()).thenReturn("localhost");
//
//    ConnPool pool = PowerMockito.mock(ConnPool.class);
//    PowerMockito.when(pool.newConn("localhost", 10045, true)).thenReturn(connection);
//
//    Connection c = pool.getConn("localhost", 10045, true);
//    pool.releaseConn(connection);
//
//    Assert.assertSame(c, connection);
  }
}
