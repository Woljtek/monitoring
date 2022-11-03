import { CollapsableSection, Label } from '@grafana/ui';
import React from 'react';

export const ElementCounter = ({
  filteredCount,
  totalCount,
}: {
  filteredCount: { [key: string]: number };
  totalCount: { [key: string]: number };
}) => {
  return (
    <CollapsableSection label="Level counts" isOpen>
      {Object.entries(totalCount).map(([key, value]) => (
        <Label key={key}>
          {key} : {filteredCount?.[key] ?? 0} / {value}
        </Label>
      ))}
    </CollapsableSection>
  );
};
