import React, { FC, useState, useContext } from 'react';
import { Button, ConfirmModal } from '@grafana/ui';
import { MutableDataFrame } from '@grafana/data';
import { unlinkInvalidations, refresh, removeOrphanedInvalidation, removeOrphanedInvalidationCompleteness } from './utils';
import { PanelContext } from 'SimplePanel';


interface Props {
  disabled: boolean;
  selectedRows: MutableDataFrame;
  table: string;
  toggleAllRowsSelected: (value?: boolean | undefined) => void;
}

export const UnlinkInvalidationButton: FC<Props> = ({ disabled, selectedRows, table, toggleAllRowsSelected }) => {
  const { dataSource, timeRange } = useContext(PanelContext);
  const [unlinkModaleIsVisible, setUnlinkModaleIsVisible] = useState(false);
  const onUnlinkClicked = (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    e.preventDefault();
    setUnlinkModaleIsVisible(true);
  };
  const productIds = selectedRows.fields.find(f => f.name === 'id')?.values.toArray() || [];
  const queryContext = { dataSource, timeRange };
  return (
    <>
      <Button onClick={onUnlinkClicked} disabled={disabled} icon="link">
        Unlink
      </Button>
      <ConfirmModal
        isOpen={unlinkModaleIsVisible}
        title="Unlink invalidation"
        body={
          <p>
            {`Are you sure you want to unlink the invalidation from this Product?`}
            <br />
            {`If no left Product is linked to this invalidation, the latter will be deleted from the database.`}
          </p>
        }
        confirmText="Unlink"
        icon="exclamation-triangle"
        onConfirm={() =>
          unlinkInvalidations(queryContext, productIds)
            .then(r => removeOrphanedInvalidationCompleteness(queryContext, 'invalidation_completeness'))
            .then(r => removeOrphanedInvalidation(queryContext, table))
            .then(r => toggleAllRowsSelected(false))
            .then(r => setUnlinkModaleIsVisible(false))
            .then(refresh)
        }
        onDismiss={() => setUnlinkModaleIsVisible(false)}
      />
    </>
  );
};
