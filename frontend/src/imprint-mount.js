import ImprintModal from './components/ImprintModal.svelte';

window.showImprintModal = function() {
  const target = document.createElement('div');
  document.body.appendChild(target);
  const modal = new ImprintModal({
    target
  });
  modal.$on('close', () => {
    modal.$destroy();
    target.remove();
  });
};
