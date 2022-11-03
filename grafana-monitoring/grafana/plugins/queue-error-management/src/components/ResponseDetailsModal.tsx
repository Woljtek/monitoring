import { Modal, ModalTabsHeader, TabContent } from '@grafana/ui';
import React, { useState } from 'react';

const tabs = [
  { label: 'Successfull', value: 'successfull', active: true },
  { label: 'Skipped', value: 'skipped', active: false },
];
export function ResponseDetailsModal({
  isOpen,
  onDismiss,
  action,
  idsWithSuccess,
  idsSkipped,
}: {
  isOpen: boolean;
  onDismiss: any;
  action: string;
  idsWithSuccess: number[];
  idsSkipped: number[];
}) {
  const [activeTab, setActiveTab] = useState('successfull');
  const modalHeader = (
    <ModalTabsHeader
      title={`${action} details`}
      icon="list-ul"
      tabs={tabs}
      activeTab={activeTab}
      onChangeTab={t => {
        setActiveTab(t.value);
      }}
    />
  );
  return (
    <div>
      <Modal title={modalHeader} isOpen={isOpen} onDismiss={onDismiss}>
        <TabContent>
          {activeTab === tabs[0].value && (
            <ul>
              {idsWithSuccess.map(id => (
                <li key={id}>{id}</li>
              ))}
            </ul>
          )}
          {activeTab === tabs[1].value && (
            <ul>
              {idsSkipped.map(id => (
                <li key={id}>{id}</li>
              ))}
            </ul>
          )}
        </TabContent>
      </Modal>
    </div>
  );
}
