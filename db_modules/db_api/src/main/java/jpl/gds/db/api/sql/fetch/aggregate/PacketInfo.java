package jpl.gds.db.api.sql.fetch.aggregate;

import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.holders.ApidNameHolder;
import jpl.gds.shared.holders.SpscHolder;
import jpl.gds.shared.holders.VcfcHolder;
import jpl.gds.shared.time.IAccurateDateTime;

public class PacketInfo {
	private ApidHolder apid;
	private ApidNameHolder apidName;
	private SpscHolder spsc;
	private IAccurateDateTime rct;
	private VcfcHolder vcfc;	
	
	public ApidHolder getApid() {
		return apid;
	}
	public void setApid(ApidHolder apid) {
		this.apid = apid;
	}
	public ApidNameHolder getApidName() {
		return apidName;
	}
	public void setApidName(ApidNameHolder apidName) {
		this.apidName = apidName;
	}
	public SpscHolder getSpsc() {
		return spsc;
	}
	public void setSpsc(SpscHolder spsc) {
		this.spsc = spsc;
	}
	public IAccurateDateTime getRct() {
		return rct;
	}
	public void setRct(IAccurateDateTime rct) {
		this.rct = rct;
	}
	public VcfcHolder getVcfc() {
		return vcfc;
	}
	public void setVcfc(VcfcHolder vcfc) {
		this.vcfc = vcfc;
	}
}
