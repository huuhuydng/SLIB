import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:icons_plus/icons_plus.dart';
import 'RegisterPage.dart';

class Loginpage extends StatefulWidget {

  @override
  _LoginpageState createState() => _LoginpageState();
}

class _LoginpageState extends State<Loginpage> {
  bool _obscureText = true;
  bool _remember = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [

          // 1. PHẦN NỀN CAM
          Column(
            children: [
              Expanded(
                flex: 3,
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.deepOrangeAccent,
                    borderRadius: BorderRadius.vertical(
                      bottom: Radius.circular(10),
                    ),
                  ),
                  child: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Image.asset(
                          "assets/images/logo_nencam.png",
                          height: 100,
                        ),
                        SizedBox(height: 15),
                        Text(
                          "Đăng nhập vào tài khoản của bạn",
                          style: TextStyle(
                            fontSize: 26,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        SizedBox(height: 10),
                        Text(
                          "Nhập email và mật khẩu để tiếp tục",
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.white,
                          ),
                        ),
                        SizedBox(height: 150,)
                      ],
                    ),
                  ),

                ),
              ),

              // PHẦN TRẮNG
              Expanded(
                flex: 2,
                child: Container(
                  width: double.infinity,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.vertical(
                      top: Radius.circular(30),
                    ),
                  ),
                ),
              ),
            ],
          ),

          // 3. FORM NỔI
          Positioned(
            top: MediaQuery.of(context).size.height * 0.34,
            left: 0,
            right: 0,
            child: Center(
              child: Container(
                width: MediaQuery.of(context).size.width * 0.9,
                padding: EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black12,
                      blurRadius: 15,
                      spreadRadius: 3,
                      offset: Offset(0, 5),
                    ),
                  ],
                ),

                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Đăng nhập với Google
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: () {},
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.white,
                          padding: EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                        ),
                        child:
                        Row(

                          mainAxisAlignment: MainAxisAlignment.start,
                          children: [
                            SizedBox(width: 30,),
                            Brand(
                              Brands.google,
                              size: 25,
                            ),
                            SizedBox(width: 60),
                            Text("Tiếp tục với Google",
                                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, fontFamily: 'Geom')),
                          ],
                        ),
                      ),
                    ),
                    SizedBox(height: 30),

                    // Đường kẻ
                    Row(
                      children: [
                        Expanded(
                          child: Divider(
                            thickness: 1,
                            color: Colors.grey,
                          ),
                        ),
                        Padding(
                          padding: EdgeInsets.symmetric(horizontal: 8),
                          child: Text("Hoặc"),
                        ),
                        Expanded(
                          child: Divider(
                            thickness: 1,
                            color: Colors.grey,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 30),

                    // Form đăng nhập
                    TextField(
                      decoration: InputDecoration(
                        labelText: "Email FPT (fpt.edu.vn)",
                        labelStyle: TextStyle(
                          color: Colors.grey,
                        ),
                        border: OutlineInputBorder(borderSide: BorderSide(color: Colors.grey),),
                      ),
                    ),
                    SizedBox(height: 15),

                    TextField(
                      obscureText: _obscureText,
                      decoration: InputDecoration(
                        labelText: "Mật khẩu",
                        labelStyle: TextStyle(
                          color: Colors.grey,
                        ),
                        border: OutlineInputBorder(borderSide: BorderSide(color: Colors.grey),),
                        suffixIcon: IconButton(
                          icon: Icon(
                            _obscureText ? Icons.visibility_off : Icons.visibility,
                            color:  Colors.grey,
                          ),
                          onPressed: () {
                            setState(() {
                              _obscureText = !_obscureText;
                            });
                          },
                        ),
                      ),
                    ),
                    SizedBox(height: 20),

                    // Ghi nhớ + Quên MK
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Checkbox(
                              value: _remember,
                              onChanged: (value) {
                                setState(() {
                                  _remember = value!;
                                });
                              },
                              side: BorderSide(
                                color: Colors.grey,  // màu viền khi chưa chọn
                              ),
                            ),

                            Text("Ghi nhớ đăng nhập",
                              style: TextStyle(
                                color: Colors.grey,
                              ),
                            ),
                          ],
                        ),
                        RichText(text: TextSpan(
                            text: "Quên mật khẩu?",
                            style: TextStyle(
                                color: Colors.orange,
                                fontWeight: FontWeight.bold
                            ),
                            recognizer: TapGestureRecognizer()
                              ..onTap = () {
                                Navigator.push(context, MaterialPageRoute(builder: (context) => Registerpage()));
                              }

                        )
                        )
                      ],
                    ),
                    SizedBox(height: 20),

                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: () {},
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.deepOrangeAccent,
                          padding: EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                        ),
                        child: Text(
                          "Đăng nhập",
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                          ),
                        ),
                      ),
                    ),
                    SizedBox(height: 20,),

                    RichText(
                      text: TextSpan(
                        text: "Bạn đã chưa có tài khoản? ",
                        style: TextStyle( fontSize: 14, color: Colors.black),
                        children: [
                          TextSpan(
                            text: "Đăng ký ngay",
                            style: TextStyle(
                              color: Colors.orange,
                              fontWeight: FontWeight.bold,
                            ),
                            recognizer: TapGestureRecognizer()
                              ..onTap = () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => Registerpage(),
                                  ),
                                );
                              },
                          ),
                        ],
                      ),
                    )
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
