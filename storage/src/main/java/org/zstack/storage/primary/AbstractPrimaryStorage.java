package org.zstack.storage.primary;

import org.zstack.core.Platform;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.header.storage.primary.PrimaryStorage;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStateEvent;

public abstract class AbstractPrimaryStorage implements PrimaryStorage {
	protected final static StateMachine<PrimaryStorageState, PrimaryStorageStateEvent> states;
	
	static {
		states = Platform.<PrimaryStorageState, PrimaryStorageStateEvent>createStateMachine();
		states.addTranscation(PrimaryStorageState.Enabled, PrimaryStorageStateEvent.disable, PrimaryStorageState.Disabled);
		states.addTranscation(PrimaryStorageState.Enabled, PrimaryStorageStateEvent.enable, PrimaryStorageState.Enabled);
		states.addTranscation(PrimaryStorageState.Enabled, PrimaryStorageStateEvent.maintain, PrimaryStorageState.Maintenance);
		states.addTranscation(PrimaryStorageState.Disabled, PrimaryStorageStateEvent.disable, PrimaryStorageState.Disabled);
		states.addTranscation(PrimaryStorageState.Disabled, PrimaryStorageStateEvent.enable, PrimaryStorageState.Enabled);
		states.addTranscation(PrimaryStorageState.Disabled, PrimaryStorageStateEvent.maintain, PrimaryStorageState.Maintenance);
		states.addTranscation(PrimaryStorageState.Maintenance, PrimaryStorageStateEvent.enable, PrimaryStorageState.Enabled);
		states.addTranscation(PrimaryStorageState.Maintenance, PrimaryStorageStateEvent.disable, PrimaryStorageState.Disabled);
		states.addTranscation(PrimaryStorageState.Maintenance, PrimaryStorageStateEvent.maintain, PrimaryStorageState.Maintenance);
	}
	
	public static PrimaryStorageState getNextState(PrimaryStorageState curr, PrimaryStorageStateEvent evt) {
		return states.getNextState(curr, evt);
	}
}
