package com.example.iotexample.actors.devicemanager.commands;

import java.util.Set;

public record ReplyDeviceList(long requestId, Set<String> ids) {
}
