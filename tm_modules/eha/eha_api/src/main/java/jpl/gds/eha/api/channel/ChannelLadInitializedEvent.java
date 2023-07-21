package jpl.gds.eha.api.channel;

import org.springframework.context.ApplicationEvent;

/**
 * Event to pass when the channel lad is created and ready to be boot strapped.
 * 
 *
 */
public class ChannelLadInitializedEvent extends ApplicationEvent {
	private static final long serialVersionUID = 1232116926992443285L;
	private final IChannelLad lad;

	public ChannelLadInitializedEvent(Object source, IChannelLad lad) {
		super(source);
		
		this.lad = lad;
	}
	
	/**
	 * @return the lad
	 */
	public IChannelLad getLad() {
		return lad;
	}
}
