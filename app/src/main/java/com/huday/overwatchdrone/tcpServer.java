package com.huday.overwatchdrone;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class tcpServer implements Runnable
{
    private Thread thread;
    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public tcpServer()
    {
        this.thread = new Thread( this );
        this.thread.setPriority( Thread.NORM_PRIORITY );
        this.thread.start();
    }

    @Override
    public void run()
    {
        // create a server socket
        try
        {
            this.serverSocket = new ServerSocket( 1919 );
        }
        catch ( IOException e )
        {
            System.out.println( "failed to start server socket" );
            e.printStackTrace();
        }

        // wait for a connection
        System.out.println( "waiting for connections..." );
        try
        {
            this.socket = serverSocket.accept();
        }
        catch ( IOException e )
        {
            System.out.println( "failed to accept" );
            e.printStackTrace();
        }
        System.out.println( "client connected" );

        // create input and output streams
        try
        {
            this.dataInputStream = new DataInputStream( new BufferedInputStream( this.socket.getInputStream() ) );
            this.dataOutputStream = new DataOutputStream( new BufferedOutputStream( this.socket.getOutputStream() ) );
        }
        catch ( IOException e )
        {
            System.out.println( "failed to create streams" );
            e.printStackTrace();
        }

        // send some test data
        try
        {
            this.dataOutputStream.writeInt( 123 );
            this.dataOutputStream.flush();
        }
        catch ( IOException e )
        {
            System.out.println( "failed to send" );
            e.printStackTrace();
        }

        // placeholder recv loop
        while ( true )
        {
            try
            {
                byte test = this.dataInputStream.readByte();
                System.out.println( "byte received: "+test );

                if ( test == 42 ) break;
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                break;
            }
        }

        System.out.println( "server thread stopped" );
    }
}