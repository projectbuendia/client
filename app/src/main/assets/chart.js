
function popup(name, pairs) {
  // var dialog = document.getElementById('obs-dialog');
  // var html = '<h2>' + name + '</h2>';
  var text = name + '\n\n';
  if (pairs.length > 0) {
    for (var i = 0; i < pairs.length; i++) {
      var value = pairs[i][0], time = pairs[i][1];
      // html += '<b>' + value + '</b> at ' + time + '<br>';
      text += time + ' – ' + value + '\n';
    }
    // html += '&nbsp;<br>Comments <input type="text" size="40">';
  } else {
    // html += 'No observations.';
    text += 'No observations.';
  }
  alert(text);
  return;
  // dialog.innerHTML = html;
  // dialog.showModal();
}

api = {
  min: function(f) {
    if (f.min !== undefined) return f.min;
    return Math.min.apply(null, api.values(f));
  },
  max: function(f) {
    if (f.max !== undefined) return f.max;
    return Math.max.apply(null, api.values(f));
  },
  values: function(f) {
    var ts = f().times, vs = [];
    for (var i = 0; i < ts.length; i++) {
      vs.push(f(ts[i]));
    }
    return vs;
  },
  first: function(f) {
    var t = f().times[0];
    return t == null ? null : {time: t, value: f(t)};
  },
  last: function(f) {
    var ts = f().times;
    var t = ts[ts.length - 1];
    return t == null ? null : {time: t, value: f(t)};
  }
};

function putConceptFuncs(conceptData) {
  for (var id in conceptData) {
    api['concept' + id] = getConceptFunc(id);
  }
}

function getConceptFunc(id) {
  var i = 0;
  var data = conceptData[id];
  var f = function(t) {
    var n = data.ts.length;
    if (t === undefined) return {times: data.ts};
    if (t === data.ts[i]) return data.vs[i];  // optimization for repeated calls
    if (t === data.ts[i + 1]) return data.vs[++i];  // optimization for loops
    if (n === 0 || t < data.ts[0]) return null;
    for (i = 0; i < n - 1; i++) {
      if (t < data.ts[i + 1]) return data.vs[i];
    }
    return data.vs[n - 1];
  };
  if (data.vs.length > 0) {
    f.min = Math.min.apply(null, data.vs);
    f.max = Math.max.apply(null, data.vs);
  }
  return f;
}

function runTileScript(conceptData, conceptIds, tileScript) {
  conceptIds = conceptIds.split(',');
  var args = [conceptIds];
  for (var i = 0; i < conceptIds.length; i++) {
    args.push(getConceptFunc(conceptIds[i]));
  }
  applyScript(tileScript, args);
}

function runGridRowScript(conceptData, conceptIds, chartRowScript) {
  conceptIds = conceptIds.split(',');
  var args = [conceptIds];
  for (var i = 0; i < conceptIds.length; i++) {
    var id = conceptIds[i];
    var data = conceptData[id];
    args.push(getConceptFunc(id));
    args.push(data.ts);
    var rowCells = document.querySelectorAll('.concept-' + id + ' td');
    var cells = [];
    for (var c = 0; c < data.cis.length; c++) {
      cells.push(rowCells[data.cis[c]]);
    }
    args.push(cells);
  }
  applyScript(chartRowScript, args);
}

function applyScript(script, args) {
  var code = '(' + script + ')';
  try {
    // Use eval so that bad syntax in one script doesn't break the whole page.
    var func = eval(code);  // should evaluate to a function
    func.apply(null, args);
  } catch (e) {
    console.log(e.stack + '\n\nin script:\n\n' + code);
  }
}
