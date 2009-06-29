package sc.server.network;

import sc.api.plugins.RescueableClientException;

public interface IClientListener
{
    public void onClientDisconnected(Client source);
	void onRequest(Client source, Object packet) throws RescueableClientException;
}
