import React, { FC, useContext, useState } from 'react';
import { Button, ConfirmModal } from '@grafana/ui';
import { MutableDataFrame } from '@grafana/data';
import { PanelContext } from 'SimplePanel';
import { refresh, removeOrphanedInvalidation, unlinkInvalidations } from './utils';

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
  const datatakeDbIds = selectedRows.fields.find(f => f.name === 'datatake_db_id')?.values.toArray() || [];
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
            {`Are you sure you want to unlink the invalidation from this Datatake?`}
            <br />
            {`If no left Datatake is linked to this invalidation, the latter will be deleted from the database.`}
          </p>
        }
        confirmText="Unlink"
        icon="exclamation-triangle"
        onConfirm={() =>
          unlinkInvalidations(queryContext, datatakeDbIds)
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
