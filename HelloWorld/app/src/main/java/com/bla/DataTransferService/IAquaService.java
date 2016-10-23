package com.bla.DataTransferService;


public interface IAquaService {
    public void log(String text);
    public void handleIncomingData(String data);
    public boolean getUseWindowsLineEndings();
    public String getDeviceName();
  //  public Settings settings = null;
}
