// Draws a dot where the automation's mouse is, so recordings show what it clicks.
(() => {
  if (window.__sgCursor) return;
  window.__sgCursor = true;

  const add = () => {
    const dot = document.createElement('div');
    dot.id = '__sg_cursor';
    dot.style.cssText = 'position:fixed;left:0;top:0;width:18px;height:18px;margin:-9px 0 0 -9px;'
      + 'border:3px solid #ff2d55;border-radius:50%;background:rgba(255,45,85,.25);'
      + 'pointer-events:none;z-index:2147483647;transition:transform .12s;display:none';
    document.documentElement.appendChild(dot);

    window.addEventListener('mousemove', e => {
      dot.style.display = 'block';
      dot.style.left = e.clientX + 'px';
      dot.style.top = e.clientY + 'px';
    }, true);
    window.addEventListener('mousedown', () => dot.style.transform = 'scale(1.8)', true);
    window.addEventListener('mouseup', () => dot.style.transform = 'scale(1)', true);
  };

  if (document.documentElement) add();
  else document.addEventListener('DOMContentLoaded', add);
})();
