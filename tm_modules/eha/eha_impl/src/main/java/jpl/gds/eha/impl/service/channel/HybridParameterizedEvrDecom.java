package jpl.gds.eha.impl.service.channel;

import jpl.gds.decom.algorithm.ChannelValueBuilder;
import jpl.gds.decom.algorithm.DecomArgs;
import jpl.gds.decom.algorithm.EvrBuilder;
import jpl.gds.decom.algorithm.IDecommutator;
import jpl.gds.dictionary.api.client.evr.EvrUtilityDictionaryManager;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.types.BitBuffer;

import java.io.OutputStream;
import java.util.*;

/**
 * Decom for event record (EVR) parameters
 */
public class HybridParameterizedEvrDecom implements IDecommutator {

    private List<EvrBuilder> evrs = new ArrayList<EvrBuilder>(1);
    Map<Long, IEvrDefinition> evrLookup;
    private long eventId = -1;

    public HybridParameterizedEvrDecom(EvrUtilityDictionaryManager dictionaryManager, SseContextFlag sseContextFlag) {
        Map<Long, IEvrDefinition> map = sseContextFlag.isApplicationSse() ?
                dictionaryManager.getSseEvrDefinitionMap() : dictionaryManager.getFswEvrDefinitionMap();
        this.evrLookup = new HashMap<>(map.size());
        this.evrLookup.putAll(map);
    }

    @Override
    public void setStaticArgs(Map<String, Object> args) {

    }

    @Override
    public void decom(BitBuffer buffer, Map<String, Object> args) {

        EvrBuilder evrb = new EvrBuilder();
        Object argValue = args.get("event_id");
        this.eventId = Optional.ofNullable(argValue).map(av -> Long.parseLong(av.toString())).orElse(this.eventId);

        evrb.setEventId(eventId);
        IEvrDefinition def = evrLookup.get(eventId);

        for (int i = 0; i < def.getArgs().size(); i++) {
            argValue = args.get("evr_param_"+i);
            if (argValue != null) {
                switch(def.getArgs().get(i).getType()) {
                    case I8:
                    case U8:
                        evrb.addArgument(Byte.valueOf(argValue.toString()));
                        break;
                    case I16:
                    case U16:
                        evrb.addArgument(Short.valueOf(argValue.toString()));
                        break;
                    case I32:
                    case U32:
                        evrb.addArgument(Integer.valueOf(argValue.toString()));
                        break;
                    case F32:
                        evrb.addArgument(Float.valueOf(argValue.toString()));
                        break;
                    case I64:
                    case U64:
                    case F64:
                        evrb.addArgument(Double.valueOf(argValue.toString()));
                        break;
                    default:
                        evrb.addArgument(argValue.toString());
                        break;
                }
            }
        }

        Sclk sclk = (Sclk) args.get(DecomArgs.SCLK);
        evrb.setSclk(sclk);

        evrs.add(evrb);

    }

    @Override
    public void decom(BitBuffer buffer, Map<String, Object> args, OutputStream outStream) {

    }

    @Override
    public List<ChannelValueBuilder> collectChannelValues() {
        return Collections.emptyList();
    }

    @Override
    public List<EvrBuilder> collectEvrs() {
        List<EvrBuilder> temp = this.evrs;
        this.evrs = new ArrayList<>(1);
        return temp;
    }
}
