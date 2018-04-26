package authmepe;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.FailedLoginEvent;
import protocolsupportpocketstuff.api.modals.ComplexForm;
import protocolsupportpocketstuff.api.modals.ModalCallback;
import protocolsupportpocketstuff.api.modals.elements.ElementResponse;
import protocolsupportpocketstuff.api.modals.elements.complex.ModalInput;
import protocolsupportpocketstuff.api.modals.elements.complex.ModalLabel;
import protocolsupportpocketstuff.api.util.PocketPlayer;

public class AuthMEPE extends JavaPlugin implements Listener {

	private AuthMeApi auth;
	private ComplexForm loginModal;
	private ComplexForm faultyLoginModal;
	private ComplexForm closeLoginModal;
	private ComplexForm registerModal;
	private ComplexForm faultyRegisterModal;
	private ComplexForm closeRegisterModal;
	private ComplexForm notMatchRegisterModal;
	private ModalCallback loginCallback;
	private ModalCallback registerCallback;
	private long modaldelay;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadConfig();
		bakeModals();
		auth = AuthMeApi.getInstance();
		bakeCallbacks();
		modaldelay = getConfig().getLong("modals.delay");
		getServer().getPluginManager().registerEvents(this, this);
	}

	//Create all the modals for easy serving.
	public void bakeModals() {
		loginModal = new ComplexForm(getConfig().getString("modals.login.title"))
				.addElement(new ModalLabel(getConfig().getString("modals.login.content")))
				.addElement(new ModalInput(" ").setPlaceholderText(getConfig().getString("modals.login.password-text")));
		faultyLoginModal = loginModal.clone();
		faultyLoginModal.getElements().get(0).setText(getConfig().getString("modals.login.content-error"));
		closeLoginModal = loginModal.clone();
		closeLoginModal.getElements().get(0).setText(getConfig().getString("modals.login.close-error"));
		registerModal = new ComplexForm(getConfig().getString("modals.register.title"))
				.addElement(new ModalLabel(getConfig().getString("modals.register.content")))
				.addElement(new ModalInput(" ").setPlaceholderText(getConfig().getString("modals.register.password-text")))
				.addElement(new ModalInput(" ").setPlaceholderText(getConfig().getString("modals.register.confirm-text")));
		faultyRegisterModal = registerModal.clone();
		faultyRegisterModal.getElements().get(0).setText(getConfig().getString("modals.register.content-error"));
		closeRegisterModal = registerModal.clone();
		closeRegisterModal.getElements().get(0).setText(getConfig().getString("modals.register.close-error"));
		notMatchRegisterModal = registerModal.clone();
		notMatchRegisterModal.getElements().get(0).setText(getConfig().getString("modals.register.confirm-error"));
	}

	public void bakeCallbacks() {
		loginCallback = response -> {
			if (!response.isCancelled()) {
				ElementResponse password = response.asComplexFormResponse().getResponse(1);
				if (auth.checkPassword(response.getPlayer().getName(), password.getString())) {
					auth.forceLogin(response.getPlayer());
				} else {
					PocketPlayer.sendModal(response.getPlayer(), faultyLoginModal, loginCallback);
				}
			} else {
				PocketPlayer.sendModal(response.getPlayer(), closeLoginModal, loginCallback);
			}
		};
        registerCallback = response -> {
            if (!response.isCancelled()) {
                ElementResponse password = response.asComplexFormResponse().getResponse(1);
                ElementResponse confirm = response.asComplexFormResponse().getResponse(2);
                if (password.getString().trim().equals(confirm.getString().trim())) {
                    if (password.getString().length() < 4 || password.getString().length() > 16) {
                        PocketPlayer.sendModal(response.getPlayer(), faultyRegisterModal, registerCallback);
                    } else {
                        auth.forceRegister(response.getPlayer(), password.getString().trim());
                        auth.forceLogin(response.getPlayer());
                    }
                } else {
                    PocketPlayer.sendModal(response.getPlayer(), notMatchRegisterModal, registerCallback);
                }
            } else {
                PocketPlayer.sendModal(response.getPlayer(), closeRegisterModal, registerCallback);
            }
        };
	}

	//Show login or register modal on login.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onLogin(PlayerJoinEvent event) {
		//We need schedular currently for not so good internals that need to 'settle' such as movment confirm thingy. :rip:
		Bukkit.getScheduler().runTaskLater(this, () -> {
			if (PocketPlayer.isPocketPlayer(event.getPlayer()) && 
					!auth.isAuthenticated(event.getPlayer())) {
					if (auth.isRegistered(event.getPlayer().getName())) {
						PocketPlayer.sendModal(event.getPlayer(), loginModal, loginCallback);
					} else {
						PocketPlayer.sendModal(event.getPlayer(), registerModal, registerCallback);
					}
				}
		}, modaldelay);
	}

	//Show faultylogin modal on login failure.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onLoginFailure(FailedLoginEvent event) {
		if (PocketPlayer.isPocketPlayer(event.getPlayer())) {
			PocketPlayer.sendModal(event.getPlayer(), faultyLoginModal, loginCallback);
		}
	}

}
