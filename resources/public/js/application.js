jQuery(function($) {

  "use strict";

  var Utils = (function() {
    function Utils() {
      var _this = this;
      this.symbols = {
        "": "",
        "EUR": "€"
      };
      this.getCurrencySymbol = function(currency) {
        return _this.symbols[currency];
      };
    };
    Utils.prototype.getUid = function(uri) {
      return uri.split("/")[4];
    };
    Utils.prototype.updateLocation = function(uri) {
      window.location = window.location + "/" + this.getUid(uri);
    };
    Utils.prototype.createProductRow = function(basket, product) {
      var $name = $("<td>"),
          $priceText = $("<td>"),
          $units = $("<td>"),
          $price = $("<input>", {type: "hidden", value: product.price}),
          $uid = $("<input>", {type: "hidden", value: product.uid}),
          $item = $("<tr>", {class: "remove-item pointer"});

      $name.text(product.name);
      $priceText.text(this.getPriceText(JSON.parse(product.price)));
      $units.text(product.units);
      $item.append($name);
      $item.append($priceText);
      $item.append($units);
      $item.append($price);
      $item.append($uid);
      $item.hover(function() {
        $(this).addClass("danger");
      }, function (e) {
        $(this).removeClass("danger");
      });
      $item.click(function(e) {
        // remove a product from a basket
        var productRow = e.currentTarget,
            price = productRow.children[productRow.children.length-2].value,
            uid = productRow.children[productRow.children.length-1].value;

        basket.removeProduct(uid, productRow, price);
      });
      return $item;
    };
    Utils.prototype.getPriceText = function(price) {
      var currency = price.amount === "0.00" ? "" : price.currency;
      var currencySymbol = this.getCurrencySymbol(currency);
      return currencySymbol + price.amount + " " + currency;
    }
    return Utils;
  })();
  var utils = new Utils();

  var Product = (function() {
    function Product(uid, name, price) {
      this.uid = uid;
      this.name = name;
      this.price = price;
      this.units = 1;
    };
    return Product;
  })();

  var Basket = (function() {
    function Basket() {
      var _this = this;
      this.data = {};
      this.data.products = {};
      this.data.total = {"amount": "0.00", "currency": ""};
      this.data.etickets = "";
      this.productList = $(".product-list");
      this.totalElement = $(".total");
      this.confirmButton = $("#basket-confirm");
      this.resetButton = $("#basket-reset");
      this.eticketsButton = $("#etickets");
      this.costsText = $("#costs");
      this.createProductRow = function(product) {
        if (_this.containsProduct(product.uid)) {
          product = _this.getProduct(product.uid);
        } else {
          _this.setProduct(product);
        }
        var productRow = utils.createProductRow(_this, product);
        _this.productList.append(productRow);
      };
      this.setProduct = function(product) {
        _this.data.products[product.uid] = product;
      };
      this.deleteProduct = function(uid) {
        delete this.data.products[uid];
      };
      this.updateProduct = function(uid) {
        var product = _this.getProduct(uid);
        product.units += 1;
      };
      this.updateTotal = function() {
        var total = 0;
        for (var uid in this.data.products) {
          var product = this.data.products[uid];
          var price = JSON.parse(product.price);
          total += parseFloat(price.amount) * product.units;
          this.data.total.currency = price.currency;
        }
        this.data.total.amount = total.toFixed(2);
        var priceText = utils.getPriceText(_this.data.total);
        this.totalElement.text(priceText);
      };
      this.clear = function() {
        this.data.products = {};
        this.data.total = {"amount": "0.00", "currency": ""};
        this.data.etickets = "";
      };
      this.confirmButton.click(function(event) {
        event.preventDefault();
        _this.confirmButton.prop("disabled", true);
        _this.resetButton.prop("disabled", true);
        $.ajax({
          type: "POST",
          url: "/basket/confirm",
          data: {
            "data": _this.data
          }
        })
        .success(function(response) {
          _this.data.etickets = response.etickets;
          _this.save();
          _this.confirmButton.hide();
          var priceText = utils.getPriceText(response.total);
          _this.totalElement.text(priceText);
          _this.costsText.show();
          _this.eticketsButton.attr("href", response.etickets).show();
          _this.resetButton.prop("disabled", false);
        })
        .fail(function(request) {
          showAlert(request.responseText);
          _this.resetButton.prop("disabled", false);
          _this.confirmButton.prop("disabled", false);
        });
      });
      this.resetButton.click(function(event) {
        event.preventDefault();
        _this.confirmButton.show();
        _this.confirmButton.prop("disabled", false);
        _this.eticketsButton.hide();
        _this.costsText.hide();
        _this.reset();
        _this.update();
      });
    };
    Basket.prototype.getProduct = function(uid) {
      return this.data.products[uid];
    };
    Basket.prototype.containsProduct = function(uid) {
      return this.data.products[uid] !== undefined;
    };
    Basket.prototype.save = function() {
      localStorage["basket"] = JSON.stringify(this.data);
    };
    Basket.prototype.load = function() {
      this.data = JSON.parse(localStorage["basket"]);
    };
    Basket.prototype.exists = function() {
      return localStorage["basket"] !== undefined;
    };
    Basket.prototype.init = function() {
      if (!this.exists()) {
        this.save();
      } else {
        this.load();
        if (this.data.etickets !== "") {
          this.confirmButton.hide();
          this.eticketsButton.attr("href", this.data.etickets).show();
        }
      }
    };
    Basket.prototype.empty = function() {
      this.productList.empty();
    };
    Basket.prototype.addProduct = function(uid, name, price) {
      if (this.data.etickets === "") {
        if (this.containsProduct(uid)) {
          this.updateProduct(uid);
          this.update();
        } else {
          var product = new Product(uid, name, price);
          this.createProductRow(product);
        }
        this.updateTotal();
        this.save();
      }
    };
    Basket.prototype.removeProduct = function(uid, productRow, price) {
      if (this.containsProduct(uid)) {
        var product = this.getProduct(uid);
        product["units"] -= 1
        if (product["units"] === 0) {
          productRow.remove();
          this.deleteProduct(uid);
        } else {
          this.update();
        }
        this.updateTotal();
        this.save();
      }
    };
    Basket.prototype.update = function() {
      var _this = this;
      this.empty();
      for (var uid in this.data.products) {
        var product = _this.data.products[uid]
        _this.createProductRow(product);
      }
      this.updateTotal();
    };
    Basket.prototype.reset = function() {
      this.clear();
      this.save();
    };
    return Basket;
  })();
  var basket = new Basket();
  basket.init();
  basket.update();

  $(".events table tbody tr").click(function(e) {
    var children = e.currentTarget.children,
        eventUri = children[children.length-1].value;

    utils.updateLocation(eventUri);
  });

  $(".products table tbody tr.add-item").click(function(e) {
    var productRow = e.currentTarget,
        name = productRow.cells[0].innerText,
        price = productRow.children[productRow.children.length-1].value,
        uid = utils.getUid(productRow.children[productRow.children.length-2].value);

    basket.addProduct(uid, name, price);
  });

  $(".products table tbody tr.add-item").hover(function(e) {
    $(this).addClass("success");
  }, function (e) {
    $(this).removeClass("success");
  });

  $("#login-form").on("submit", function(event) {
    event.preventDefault();
    var data = $(this).serializeArray();
    var email = data[0].value;
    var password = data[1].value;
    var next = window.location.href;
    $.ajax({
      type: "POST",
      url: "/profile/login",
      data: {
        "email": email,
        "password": password
      }
    })
    .success(function(response) {
      if (response.error) {
        showAlert(response.message);
      } else {
        window.location.href = next;
      }
    })
    .fail(function(request) {
      showAlert(request.responseText);
    });
  });

  $("#logout").click(function(event) {
    event.preventDefault();
    var href = $(this).attr("href");
    $.ajax({
      type: "GET",
      url: href
    })
    .success(function(response) {
      basket.reset();
      window.location.pathname = "/";
    })
    .fail(function(request) {
      showAlert(request.responseText);
    });
  })

  var showAlert = function(message) {
    var messageBox = $("#message");
    messageBox.html(" \
      <div class=\"alert alert-warning alert-dismissable\"> \
        <button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-hidden=\"true\">&times;</button> \
        " + message + " \
      </div> \
    ");
  };

  $(".datepicker").datepicker();





  // related to PeerJS, not yet functioning
  var Connection = (function() {
    function Connection(peerID) {
      var _this = this;
      this.conn;
      this.peer = new Peer(peerID, {host: 'localhost', port: 8000, path: '/'});
      this.peer.on("connection", function(conn) {
        conn.on('data', function(data) {
          console.log(data);
        });
      });
    };
    Connection.prototype.connect = function(peerID) {
      this.conn = peer.connect(peerID);
    };
    Connection.prototype.send = function(data) {
      if (this.conn) {
        console.log("send " + data);
        this.conn.send(data);
      } else {
        console.log("no connection established");
      }
    };
    return Connection;
  })();

  var peer;
  var connection;

  $("#peer").click(function(event) {
    var myPeerID = $("#my-peer-id").val();
    peer = new Peer(myPeerID, {host: 'localhost', port: 8000, path: '/'});
    peer.on("connection", function(conn) {
      conn.on('data', function(data) {
        console.log(data);
      });
    });
  });
  $("#connect").click(function(event) {
    if (peer) {
      var otherPeerID = $("#other-peer-id").val();
      connection = peer.connect(otherPeerID);
    }
  });
  $("#send").click(function(event) {
    if (connection) {
      var data = $("#data").val();
      connection.send(data);
    }
  });
});
