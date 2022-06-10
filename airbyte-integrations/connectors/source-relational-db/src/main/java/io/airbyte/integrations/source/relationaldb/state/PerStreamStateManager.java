/*
 * Copyright (c) 2022 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.relationaldb.state;

import static io.airbyte.integrations.source.relationaldb.state.StateGeneratorUtils.CURSOR_FIELD_FUNCTION;
import static io.airbyte.integrations.source.relationaldb.state.StateGeneratorUtils.CURSOR_FUNCTION;
import static io.airbyte.integrations.source.relationaldb.state.StateGeneratorUtils.NAME_NAMESPACE_PAIR_FUNCTION;

import io.airbyte.commons.json.Jsons;
import io.airbyte.integrations.base.AirbyteStreamNameNamespacePair;
import io.airbyte.integrations.source.relationaldb.CdcStateManager;
import io.airbyte.integrations.source.relationaldb.CursorInfo;
import io.airbyte.protocol.models.AirbyteStateMessage;
import io.airbyte.protocol.models.AirbyteStateMessage.AirbyteStateType;
import io.airbyte.protocol.models.AirbyteStreamState;
import io.airbyte.protocol.models.ConfiguredAirbyteCatalog;
import java.util.List;
import java.util.Map;

/**
 * Per-stream implementation of the {@link StateManager} interface.
 *
 * This implementation generates a state object for each stream detected in catalog/map of known
 * streams to cursor information stored in this manager.
 */
public class PerStreamStateManager extends AbstractStateManager<AirbyteStateMessage, AirbyteStreamState> {

  /**
   * Constructs a new {@link PerStreamStateManager} that is seeded with the provided
   * {@link AirbyteStateMessage}.
   *
   * @param airbyteStateMessage The initial state represented as an {@link AirbyteStateMessage}.
   * @param catalog The {@link ConfiguredAirbyteCatalog} for the connector associated with this state
   *        manager.
   */
  public PerStreamStateManager(final AirbyteStateMessage airbyteStateMessage, final ConfiguredAirbyteCatalog catalog) {
    super(catalog,
        () -> airbyteStateMessage.getStreams(),
        CURSOR_FUNCTION,
        CURSOR_FIELD_FUNCTION,
        NAME_NAMESPACE_PAIR_FUNCTION);
  }

  @Override
  public CdcStateManager getCdcStateManager() {
    return new CdcStateManager(null);
  }

  @Override
  public AirbyteStateMessage toState() {
    final Map<AirbyteStreamNameNamespacePair, CursorInfo> pairToCursorInfoMap = getPairToCursorInfoMap();
    final AirbyteStateMessage airbyteStateMessage = new AirbyteStateMessage();
    final List<AirbyteStreamState> airbyteStreamStates = StateGeneratorUtils.generatePerStreamState(pairToCursorInfoMap);
    return airbyteStateMessage
        .withStateType(AirbyteStateType.PER_STREAM)
        // Temporarily include legacy state for backwards compatibility with the platform
        .withData(Jsons.jsonNode(StateGeneratorUtils.generateDbState(pairToCursorInfoMap)))
        .withStreams(airbyteStreamStates);
  }
}