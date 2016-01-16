package com.bla.DataTransferService;



interface ILogger {
    void onLog(String msg);

    void onLogAsync(String msg);
}
