package Bank.enumer;

public enum ClientSequence {
	REQUEST_CONNECTION,
    SEND_USERNAME,
    SEND_PASSWORD,
    SEND_COMMAND,
    FIN,
    FIN_ACK;
}
