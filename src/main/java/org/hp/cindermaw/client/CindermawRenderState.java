package org.hp.cindermaw.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.hp.cindermaw.entity.CindermawAttack;
import org.hp.cindermaw.entity.CindermawEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.dataticket.OverridingDataTicket;
import com.geckolib.renderer.base.GeoRenderState;
public final class CindermawRenderState extends LivingEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckolibData = new HashMap<>();
    public CindermawEntity entity;
    public CindermawAttack attack = CindermawAttack.NONE;
    public float attackAge;
    public float partial;
    public float time;
    public boolean phase2;
    public boolean dying;
    public int fireDrawn;

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return geckolibData;
    }

    @Override
    public <D> void addGeckolibData(DataTicket<D> ticket, D data) {
        geckolibData.put(ticket, data);
    }

    @Override
    public boolean hasGeckolibData(DataTicket<?> ticket) {
        return geckolibData.containsKey(ticket);
    }

    @Override
    public <D> D getGeckolibData(DataTicket<D> ticket) {
        D stored = read(ticket);
        if (stored != null) {
            return stored;
        }
        return overridden(ticket);
    }

    @Override
    public <D> D getOrDefaultGeckolibData(DataTicket<D> ticket, D fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(DataTicket<D> ticket, Supplier<D> fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback.get();
    }

    @SuppressWarnings("unchecked")
    private <D> D read(DataTicket<D> ticket) {
        return (D) geckolibData.get(ticket);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <D> D overridden(DataTicket<D> ticket) {
        if (ticket instanceof OverridingDataTicket overriding && overriding.canExtractFrom(this)) {
            return (D) overriding.extractFrom(overriding.getOverriddenClass().cast(this));
        }
        return null;
    }
}
