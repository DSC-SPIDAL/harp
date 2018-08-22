package edu.iu.harp.io;

import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;

import java.net.Socket;

public class ConnectionTest {
  @Test
  public void testConnection() throws Exception {
    Socket socket = PowerMockito.mock(Socket.class);
    PowerMockito.whenNew(Socket.class).withAnyArguments().thenReturn(socket);
    PowerMockito.doNothing().when(socket).connect(Matchers.anyObject(), Matchers.anyInt());


  }
}
