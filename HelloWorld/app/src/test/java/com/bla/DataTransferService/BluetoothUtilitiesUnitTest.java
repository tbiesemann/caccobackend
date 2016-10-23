package com.bla.DataTransferService;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import android.content.SharedPreferences;
import com.bla.DataTransferService.BluetoothUtilities;

import java.util.ArrayList;

import static org.junit.Assert.*;

class AquaServiceMock implements IAquaService{
    public int handleIncomingDataCallcount = 0;
    public ArrayList<String> handleIncomingDataArgs = new ArrayList<>();
    public boolean useWindowsLineEndings = true;
    public AquaServiceMock(){
    }
    public void log(String text){

    }
    public void handleIncomingData(String data){
        this.handleIncomingDataCallcount++;
        this.handleIncomingDataArgs.add(data);
    }
    public boolean getUseWindowsLineEndings(){
        return useWindowsLineEndings;
    }
    public String getDeviceName(){
        return "MyAndroidSmartphone";
    }
}


@RunWith(MockitoJUnitRunner.class)
public class BluetoothUtilitiesUnitTest {
    private AquaServiceMock mAquaServiceMock;
    BluetoothUtilities cut;


    private void receiveBytes(String dataString) throws Exception{
        byte[] data = dataString.getBytes("US-ASCII");
        cut.handleBytesReceived(data, dataString.length());
    }


    @Before
    public void initialize() {
        mAquaServiceMock = new AquaServiceMock();
         cut = new BluetoothUtilities(mAquaServiceMock);
    }

    @Test
    public void createBluetoothUtilties() throws Exception {
        assertNotNull(cut);
    }

    @Test
    public void handleBytesReceived_SingleLine() throws Exception {
        receiveBytes("First\r\nblabla");

        assertEquals(mAquaServiceMock.handleIncomingDataCallcount, 1);
        assertEquals(mAquaServiceMock.handleIncomingDataArgs.get(0), "First\r\n");
    }

    @Test
    public void handleBytesReceived_MultipleLines() throws Exception {
        receiveBytes("First\r\nSecond\r\nblabla");

        assertEquals(mAquaServiceMock.handleIncomingDataCallcount, 2);
        assertEquals(mAquaServiceMock.handleIncomingDataArgs.get(0), "First\r\n");
        assertEquals(mAquaServiceMock.handleIncomingDataArgs.get(1), "Second\r\n");
    }

    @Test
    public void handleBytesReceived_splittedLinefeed() throws Exception {
        receiveBytes("First\r");
        receiveBytes("\nblabla");
        assertEquals(mAquaServiceMock.handleIncomingDataCallcount, 1);
        assertEquals(mAquaServiceMock.handleIncomingDataArgs.get(0), "First\r\n");
    }
    @Test
    public void handleBytesReceived_Incomplete() throws Exception {
        receiveBytes("Fir");
        receiveBytes("st\r\nblabla");
        assertEquals(mAquaServiceMock.handleIncomingDataCallcount, 1);
        assertEquals(mAquaServiceMock.handleIncomingDataArgs.get(0), "First\r\n");
    }

    @Test
    public void handleBytesReceived_EmptyLines() throws Exception {
        receiveBytes("First\r\n\r");
        receiveBytes("\nblabla");
        assertEquals(mAquaServiceMock.handleIncomingDataCallcount, 2);
        assertEquals(mAquaServiceMock.handleIncomingDataArgs.get(0), "First\r\n");
        assertEquals(mAquaServiceMock.handleIncomingDataArgs.get(1), "\r\n");
    }
}