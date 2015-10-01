var console = console || {
  log: function(message) {
    java.lang.System.out.println(JSON.stringify(message, null, 2));
  }
};

/**
 * https://github.com/bramstein/url-template
 */
(function (root, factory) {
    if (typeof exports === 'object') {
        module.exports = factory();
    } else if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else {
        root.urltemplate = factory();
    }
}(this, function () {
  /**
   * @constructor
   */
  function UrlTemplate() {
  }

  /**
   * @private
   * @param {string} str
   * @return {string}
   */
  UrlTemplate.prototype.encodeReserved = function (str) {
    return str.split(/(%[0-9A-Fa-f]{2})/g).map(function (part) {
      if (!/%[0-9A-Fa-f]/.test(part)) {
        part = encodeURI(part);
      }
      return part;
    }).join('');
  };

  /**
   * @private
   * @param {string} operator
   * @param {string} value
   * @param {string} key
   * @return {string}
   */
  UrlTemplate.prototype.encodeValue = function (operator, value, key) {
    value = (operator === '+' || operator === '#') ? this.encodeReserved(value) : encodeURIComponent(value);

    if (key) {
      return encodeURIComponent(key) + '=' + value;
    } else {
      return value;
    }
  };

  /**
   * @private
   * @param {*} value
   * @return {boolean}
   */
  UrlTemplate.prototype.isDefined = function (value) {
    return value !== undefined && value !== null;
  };

  /**
   * @private
   * @param {string}
   * @return {boolean}
   */
  UrlTemplate.prototype.isKeyOperator = function (operator) {
    return operator === ';' || operator === '&' || operator === '?';
  };

  /**
   * @private
   * @param {Object} context
   * @param {string} operator
   * @param {string} key
   * @param {string} modifier
   */
  UrlTemplate.prototype.getValues = function (context, operator, key, modifier) {
    var value = context[key],
        result = [];

    if (this.isDefined(value) && value !== '') {
      if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
        value = value.toString();

        if (modifier && modifier !== '*') {
          value = value.substring(0, parseInt(modifier, 10));
        }

        result.push(this.encodeValue(operator, value, this.isKeyOperator(operator) ? key : null));
      } else {
        if (modifier === '*') {
          if (Array.isArray(value)) {
            value.filter(this.isDefined).forEach(function (value) {
              result.push(this.encodeValue(operator, value, this.isKeyOperator(operator) ? key : null));
            }, this);
          } else {
            Object.keys(value).forEach(function (k) {
              if (this.isDefined(value[k])) {
                result.push(this.encodeValue(operator, value[k], k));
              }
            }, this);
          }
        } else {
          var tmp = [];

          if (Array.isArray(value)) {
            value.filter(this.isDefined).forEach(function (value) {
              tmp.push(this.encodeValue(operator, value));
            }, this);
          } else {
            Object.keys(value).forEach(function (k) {
              if (this.isDefined(value[k])) {
                tmp.push(encodeURIComponent(k));
                tmp.push(this.encodeValue(operator, value[k].toString()));
              }
            }, this);
          }

          if (this.isKeyOperator(operator)) {
            result.push(encodeURIComponent(key) + '=' + tmp.join(','));
          } else if (tmp.length !== 0) {
            result.push(tmp.join(','));
          }
        }
      }
    } else {
      if (operator === ';') {
        result.push(encodeURIComponent(key));
      } else if (value === '' && (operator === '&' || operator === '?')) {
        result.push(encodeURIComponent(key) + '=');
      } else if (value === '') {
        result.push('');
      }
    }
    return result;
  };

  /**
   * @param {string} template
   * @return {function(Object):string}
   */
  UrlTemplate.prototype.parse = function (template) {
    var that = this;
    var operators = ['+', '#', '.', '/', ';', '?', '&'];

    return {
      expand: function (context) {
        return template.replace(/\{([^\{\}]+)\}|([^\{\}]+)/g, function (_, expression, literal) {
          if (expression) {
            var operator = null,
                values = [];

            if (operators.indexOf(expression.charAt(0)) !== -1) {
              operator = expression.charAt(0);
              expression = expression.substr(1);
            }

            expression.split(/,/g).forEach(function (variable) {
              var tmp = /([^:\*]*)(?::(\d+)|(\*))?/.exec(variable);
              values.push.apply(values, that.getValues(context, operator, tmp[1], tmp[2] || tmp[3]));
            });

            if (operator && operator !== '+') {
              var separator = ',';

              if (operator === '?') {
                separator = '&';
              } else if (operator !== '#') {
                separator = operator;
              }
              return (values.length !== 0 ? operator : '') + values.join(separator);
            } else {
              return values.join(',');
            }
          } else {
            return that.encodeReserved(literal);
          }
        });
      }
    };
  };

  return new UrlTemplate();
}));

Handlebars.registerHelper('getField', function (string, options) {
  var parts = string.split('.');
  return parts[parts.length -1];
});

Handlebars.registerHelper('getIcon', function (string, options) {
  var icons = {
    'service': 'desktop',
    'person': 'user',
    'organization': 'users',
    'article': 'comment',
    'action': 'gears',
    'concept': 'bars',
    'conceptscheme': 'envelope'
  };
  return new Handlebars.SafeString('<i class="fa fa-' + (icons[string.toLowerCase()] || 'desktop') + '"></i>');
});

Handlebars.registerHelper('json', function (obj, options) {
  return new Handlebars.SafeString(JSON.stringify(obj, null, 2));
});

Handlebars.registerHelper('getBundle', function (field, options) {
  var bundles = {
    'availableLanguage': 'languages',
    'addressCountry': 'countries'
  }
  return bundles[field] || 'messages';
});

Handlebars.registerHelper('getResourceUrl', function (url, options) {
  return new Handlebars.SafeString("/resource/" + encodeURIComponent(url));
});

/**
 * Adopted from http://stackoverflow.com/questions/7261318/svg-chart-generation-in-javascript
 */
  Handlebars.registerHelper('pieChart', function(aggregation, options) {

  // FIXME actually an array is passed in , but rhino does not recognize it as such?
  var buckets = [];
  for (bucket in aggregation['buckets']) {
    buckets.push(aggregation['buckets'][bucket]);
  }

  var width = options.hash.width || 400;
  var height = options.hash.height || 400;


  function openTag(type, closing, attr) {
      var html = ['<' + type];
      for (var prop in attr) {
        // A falsy value is used to remove the attribute.
        // EG: attr[false] to remove, attr['false'] to add
        if (attr[prop]) {
          html.push(prop + '="' + attr[prop] + '"');
        }
      }
      return html.join(' ') + (!closing ? ' /' : '') + '>';
    }

  function closeTag(type) {
    return '</' + type + '>';
  }

  function createElement(type, closing, attr, contents) {
    return openTag(type, closing, attr) + (closing ? (contents || '') + closeTag(type) : '');
  }

  var total = buckets.reduce(function (accu, that) {
    return that['doc_count'] + accu;
  }, 0);

  var sectorAngleArr = buckets.map(function (v) { return 360 * v['doc_count'] / total; });

  var arcs = [];
  var startAngle = 0;
  var endAngle = 0;
  for (var i=0; i<sectorAngleArr.length; i++) {
    startAngle = endAngle;
    endAngle = startAngle + sectorAngleArr[i];

    var x1,x2,y1,y2 ;

    x1 = parseInt(Math.round((width/2) + ((width/2)*.975)*Math.cos(Math.PI*startAngle/180)));
    y1 = parseInt(Math.round((height/2) + ((height/2)*.975)*Math.sin(Math.PI*startAngle/180)));

    x2 = parseInt(Math.round((width/2) + ((width/2)*.975)*Math.cos(Math.PI*endAngle/180)));
    y2 = parseInt(Math.round((height/2) + ((height/2)*.975)*Math.sin(Math.PI*endAngle/180)));

    var d = "M" + (width/2) + "," + (height/2)+ "  L" + x1 + "," + y1 + "  A" + ((width/2)*.975) + "," + ((height/2)*.975) + " 0 " +
        ((endAngle-startAngle > 180) ? 1 : 0) + ",1 " + x2 + "," + y2 + " z";

    var c = parseInt((i + 200) / sectorAngleArr.length * 360);
    var path = createElement("path", true, {d: d, fill: "hsl(" + c + ", 66%, 50%)"});

    var href = urltemplate.parse(options.hash['href-template']).expand(buckets[i]);
    var arc = createElement("a", true, {
      "xlink:href": href,
      "xlink:title": buckets[i]['key'] + " (" + buckets[i]['doc_count'] + ")"
    }, path);
    arcs.push(arc);
  }
  return new Handlebars.SafeString(createElement("svg" , true, {
    width: width,
    height: height,
    xmlns: "http://www.w3.org/2000/svg",
    "xmlns:xlink": "http://www.w3.org/1999/xlink"
  }, arcs.join("")));

});