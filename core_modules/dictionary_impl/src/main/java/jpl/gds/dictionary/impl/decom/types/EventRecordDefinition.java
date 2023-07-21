package jpl.gds.dictionary.impl.decom.types;

import jpl.gds.dictionary.api.decom.params.DecomDataParams;
import jpl.gds.dictionary.api.decom.types.IDecomDataDefinition;
import jpl.gds.dictionary.api.decom.types.IEventRecordDefinition;

/**
 * Class for event-record definitions
 */
public class EventRecordDefinition extends BaseDecomDataDefinition
                                   implements IDecomDataDefinition, IEventRecordDefinition {

    public EventRecordDefinition(DecomDataParams decomDataParams) {
        super(decomDataParams);
    }
}
