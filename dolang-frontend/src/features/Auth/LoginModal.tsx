import React, { useState } from 'react';
import { css } from '@emotion/react';
import { Button, Modal } from 'antd';
import GoogleLoginModal from './GoogleLoginModal';

const LogInModal: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  const showModal = () => {
    setIsModalOpen(true);
  };

  const handleOk = () => {
    setIsModalOpen(false);
  };

  const handleCancel = () => {
    setIsModalOpen(false);
  };

  return (
    <>
      <Button
        style={{
          backgroundColor: '#202022',
          color: '#ffffff',
          width: '80px',
          height: '35px',
          border: '1px solid #a0a0a0',
        }}
        onClick={showModal}
      >
        Login
      </Button>
      {/* <Modal open={isModalOpen} onOk={handleOk} onCancel={handleCancel} width={400} height={450}> */}
      <Modal open={isModalOpen} onOk={handleOk} onCancel={handleCancel} footer={null} width={400} height={450}>
        <GoogleLoginModal />
      </Modal>
    </>
  );
};

export default LogInModal;
