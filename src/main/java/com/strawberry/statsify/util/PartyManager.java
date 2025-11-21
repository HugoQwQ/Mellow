package com.strawberry.statsify.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PartyManager {

    private static final PartyManager INSTANCE = new PartyManager();
    private final Set<UUID> partyMembers = new HashSet<>();
    private boolean inParty = false;
    public static final Logger LOGGER = LogManager.getLogger("Statsify");

    private PartyManager() {
        HypixelModAPI.getInstance().registerHandler(
            ClientboundPartyInfoPacket.class,
            this::handlePartyPacket
        );
    }

    private void handlePartyPacket(ClientboundPartyInfoPacket partyInfoPacket) {
        this.partyMembers.clear();
        if (partyInfoPacket.isInParty()) {
            inParty = true;
            LOGGER.info("IN PARTY");
            partyMembers.addAll(partyInfoPacket.getMembers());
        } else {
            inParty = false;
            LOGGER.info("NOT IN PARTY");
        }
    }

    public static PartyManager getInstance() {
        return INSTANCE;
    }

    public boolean inParty() {
        return inParty;
    }

    public boolean isPartyMember(UUID uuid) {
        return partyMembers.contains(uuid);
    }

    public Set<UUID> getPartyMembers() {
        LOGGER.info("PARTY MEMBERS: " + partyMembers);
        return partyMembers;
    }
}
