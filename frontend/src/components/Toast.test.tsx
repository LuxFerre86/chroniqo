import { act, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ToastProvider, useToast } from './Toast';

function Trigger() {
  const toast = useToast();
  return (
    <>
      <button onClick={() => toast.error('Same problem')}>error</button>
      <button onClick={() => toast.success('Done')}>success</button>
    </>
  );
}

describe('Toast', () => {
  it('does not stack identical messages', () => {
    render(
      <ToastProvider>
        <Trigger />
      </ToastProvider>,
    );

    act(() => {
      screen.getByText('error').click();
      screen.getByText('error').click();
      screen.getByText('success').click();
    });

    expect(screen.getAllByText('Same problem')).toHaveLength(1);
    expect(screen.getAllByText('Done')).toHaveLength(1);
  });
});
