import React from 'react';
import { CommentOutlined, CustomerServiceOutlined } from '@ant-design/icons';
import { FloatButton } from 'antd';

const FloatingButton: React.FC = () => (
  <>
    <FloatButton.Group trigger="click" type="primary" style={{ insetInlineEnd: 24 }} icon={<CustomerServiceOutlined />}>
      <FloatButton />
      <FloatButton icon={<CommentOutlined />} />
    </FloatButton.Group>
  </>
);

export default FloatingButton;
