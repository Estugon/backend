package sc.server.network;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.api.plugins.exceptions.GameLogicException;
import sc.api.plugins.exceptions.NotYourTurnException;
import sc.api.plugins.exceptions.RescuableClientException;
import sc.networking.INetworkInterface;
import sc.networking.UnprocessedPacketException;
import sc.networking.clients.XStreamClient;
import sc.protocol.ProtocolPacket;
import sc.protocol.RemovedFromGame;
import sc.protocol.responses.ErrorPacket;
import sc.server.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A generic client. This represents a client in the server.
 *
 * Clients which connect to the server (as separate programs or running as threads started by the server)
 * are represented by {@link sc.networking.clients.LobbyClient}.
 */
public class Client extends XStreamClient implements IClient {
  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  private boolean notifiedOnDisconnect = false;
  private IClientRequestListener requestHandler = null;
  private final List<IClientListener> clientListeners = new ArrayList<>();
  private final Collection<IClientRole> roles = new ArrayList<>();

  public Client(INetworkInterface networkInterface) throws IOException {
    super(networkInterface);
  }

  public void setRequestHandler(IClientRequestListener handler) {
    requestHandler = handler;
  }

  /** @return roles of this client. */
  public Collection<IClientRole> getRoles() {
    return Collections.unmodifiableCollection(this.roles);
  }

  /** Add another role to the client. */
  @Override
  public void addRole(IClientRole role) {
    synchronized(roles) {
      this.roles.add(role);
    }
  }

  /** Call listeners upon disconnect. */
  private void notifyOnDisconnect() {
    if (!this.notifiedOnDisconnect) {
      this.notifiedOnDisconnect = true;
      for (IClientListener listener : new ArrayList<>(clientListeners)) {
        try {
          listener.onClientDisconnected(this);
        } catch (Exception e) {
          logger.error("OnDisconnect Notification caused an exception.", e);
        }
      }
    }
  }

  /** Add a {@link IClientListener listener} to the client. */
  public void addClientListener(IClientListener listener) {
    this.clientListeners.add(listener);
  }

  public void removeClientListener(IClientListener listener) {
    this.clientListeners.remove(listener);
  }

  /**
   * Test if this client is an administrator.
   *
   * @return true iff this client has an AdministratorRole
   */
  public boolean isAdministrator() {
    synchronized(roles) {
      for (IClientRole role : this.roles) {
        if (role instanceof AdministratorRole) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Authenticates a Client as Administrator
   *
   * @param password The secret that is required to gain administrative rights.
   */
  public void authenticate(String password) throws AuthenticationFailedException {
    String correctPassword = Configuration.getAdministrativePassword();

    if (correctPassword != null && correctPassword.equals(password)) {
      if (!isAdministrator()) {
        addRole(new AdministratorRole(this));
        logger.info("Client authenticated as administrator");
      } else {
        logger.warn("Client tried to authenticate as administrator twice.");
      }
    } else {
      logger.warn("Client failed to authenticate as administrator.");

      throw new AuthenticationFailedException();
    }
  }

  /**
   * Disconnect the client and cleanup.
   *
   * @param cause of disconnect
   */
  @Override
  protected void onDisconnect(DisconnectCause cause) {
    synchronized(roles) {
      for (IClientRole role : this.roles) {
        try {
          role.disconnect(cause);
        } catch (Exception e) {
          logger.warn("Couldn't close role.", e);
        }
      }
    }

    notifyOnDisconnect();
  }

  @Override
  protected void onObject(@NotNull ProtocolPacket message) throws UnprocessedPacketException {
    /*
     * NOTE that this method is called in the receiver thread. Messages
     * should only be passed to listeners. No callbacks should be invoked
     * directly in the receiver thread.
     */
    Collection<RescuableClientException> errors = new ArrayList<>();

    PacketCallback callback = new PacketCallback(message);

    try {
      requestHandler.onRequest(this, callback);
    } catch (RescuableClientException e) {
      errors.add(e);
    }

    if (errors.isEmpty() && !callback.isProcessed()) {
      String msg = String.format("Packet %s wasn't processed.", message);
      logger.warn(msg);
      throw new UnprocessedPacketException(msg);
    }

    for (RescuableClientException error : errors) {
      logger.warn("An error occured: ", error);
      send(new ErrorPacket(message, error.toString()));
      if (error instanceof GameLogicException && !(error instanceof NotYourTurnException)) {
        logger.warn("Game closed because of GameLogicException: " + error.getMessage());
      }
    }
    if (!errors.isEmpty()) {
      logger.debug("Stopping {} because of error", this);
      stop();
    }

    if (message instanceof RemovedFromGame) {
      logger.debug("Stopping {} because of received RemovedFromGame message", this);
      stop();
    }
  }

}
