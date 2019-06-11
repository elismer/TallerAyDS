import React from 'react';
import {
  AsyncStorage,
  View,
  Text,
  TextInput,
  Button,
  TouchableOpacity,
  StyleSheet,
} from 'react-native';
import axios from "../utils/axios";

export default class CommentScreen extends React.Component{
  static navigationOptions = {
    title: 'COMENTARIOS'
  };
  constructor(props) {
  super(props);
  this.state = {text: ''};
  }
  render (){
    return (
      <View style={{padding: 10}}>
         <TextInput
           style={{height: 40}}
           placeholder="Escriba aqui su comentario"
           onChangeText={(text) => this.setState({text})}
         />
        <Button
          onPress={this.onPressCommentButton.bind(this.state.text)}
          title="GUARDAR"
          style={styles.welcomeContainer}
        />
      </View>
    );
  }
  onPressCommentButton=text => {
    console.log(this.state.text);
    axios.post("/comments",{description: text}).then(response => {
      alert('Comentario enviado');
      this.props.navigation.goBack();
    });
  };
}
const styles = StyleSheet.create({
  developmentModeText: {
    marginBottom: 20,
    color: 'rgba(0,0,0,0.4)',
    fontSize: 14,
    lineHeight: 19,
    textAlign: 'center',
  },
  welcomeContainer: {
    alignItems: "center",
    marginTop: 10,
    marginBottom: 20
  }
});
