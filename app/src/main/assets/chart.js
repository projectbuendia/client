
// Displays a popup with a detailed list of observations.
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

// Each concept is abstracted as a "concept function" that satisfies the contract:
//   - f() returns an object with metadata about the concept.  f().times is an
//     an array of timestamps at which the value of the concept changes;
//     the value is piecewise constant between these timestamps.
//   - f(t) returns the value of the concept at timestamp t, for any t.
// All timestamps are in integer milliseconds since 1970-01-01 00:00:00 UTC.

// The API exposed to JS functions in the profile.  Everything provided for use
// by functions in the profile should be a member of this global api object.
api = {
  min: function(f) {
    if (f == null) return null;
    if (f.min !== undefined) return f.min;
    var values = api.values(f);
    return values.length > 0 ? Math.min.apply(null, values) : null;
  },
  max: function(f) {
    if (f == null) return null;
    if (f.max !== undefined) return f.max;
    var values = api.values(f);
    return values.length > 0 ? Math.max.apply(null, values) : null;
  },
  values: function(f) {
    if (f == null) return null;
    var ts = f().times, vs = [];
    for (var i = 0; i < ts.length; i++) {
      vs.push(f(ts[i]));
    }
    return vs;
  },
  first: function(f) {
    if (f == null) return null;
    var t = f().times[0];
    return t == null ? null : {time: t, value: f(t)};
  },
  last: function(f) {
    if (f == null) return null;
    var ts = f().times;
    var t = ts[ts.length - 1];
    return t == null ? null : {time: t, value: f(t)};
  }
};

// Adds a concept function named "conceptNNN" for all concepts in conceptData.
function putConceptFuncs(conceptData) {
  for (var id in conceptData) {
    api['concept' + id] = getConceptFunc(id);
  }
}

// Constructs the concept function for the given concept ID.
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
  if (data.vs.length > 0) {  // optimization for piecewise constant functions
    f.min = Math.min.apply(null, data.vs);
    f.max = Math.max.apply(null, data.vs);
  }
  return f;
}

// Runs a tile script with args (ids, tile, f1, f2, f3, ...)
function runTileScript(conceptData, conceptIds, tileScript) {
  conceptIds = conceptIds.split(',');
  var args = [conceptIds];
  args.push(document.getElementById('tile-' + conceptIds[0]));
  for (var i = 0; i < conceptIds.length; i++) {
    args.push(getConceptFunc(conceptIds[i]));
  }
  applyScript(tileScript, args);
}

// Runs a grid row script with args (ids, f1, ts1, cells1, f2, ts2, cells2, ...)
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

// Executes a user script using eval, to keep syntax errors from breaking the whole page.
function applyScript(script, args) {
  var code = '(' + script + ')';
  try {
    var func = eval(code);  // should evaluate to a function
    func.apply(null, args);
  } catch (e) {
    console.log(e.stack + '\n\nin script:\n\n' + code);
  }
}
