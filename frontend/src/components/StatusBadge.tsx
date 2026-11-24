import { STATUS_LABEL, isProcessing, type RecordStatus } from '../types';

export default function StatusBadge({ status }: { status: RecordStatus }) {
  return (
    <span className={`badge ${status.toLowerCase()}`}>
      {isProcessing(status) && <span className="spinner" aria-hidden />}
      {STATUS_LABEL[status]}
    </span>
  );
}
