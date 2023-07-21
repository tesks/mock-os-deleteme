package jpl.gds.db.api.sql.order;

public interface IChannelAggregateOrderByType extends IDbOrderByType {

	/** NONE_TYPE */
	int NONE_TYPE = 0;
	/** ERT_TYPE */
	int ERT_TYPE = 1;
	/** SCLK_TYPE */
	int SCLK_TYPE = 2;
	/** SCET_TYPE */
	int SCET_TYPE = 3;
	/** RCT_TYPE */
	int RCT_TYPE = 4;
	/** LST_TYPE */
	int LST_TYPE = 5;
	/** CHANNEL_ID_TYPE */
	int CHANNEL_ID_TYPE = 6;
	/** MODULE_TYPE */
	int MODULE_TYPE = 7;

}
